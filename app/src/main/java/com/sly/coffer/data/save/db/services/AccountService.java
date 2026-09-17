package com.sly.coffer.data.save.db.services;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.sqlite.db.SimpleSQLiteQuery;

import com.sly.coffer.auxiliary.classes.CustomDateTimeFormatter;
import com.sly.coffer.data.save.db.BookkeepingDb;
import com.sly.coffer.data.save.db.converters.DateTimeConverter;
import com.sly.coffer.data.save.db.daos.AccountDao;
import com.sly.coffer.data.save.db.entities.AccountEntity;
import com.sly.coffer.data.save.db.entities.AccountTransferEntity;
import com.sly.coffer.data.save.db.entities.MediaEntity;
import com.sly.coffer.data.save.db.entities.composite.ui.AccountUiModel;
import com.sly.coffer.helpers.file.FileHelper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

public class AccountService {
    /**
     * 加载流水记录列表数据
     *
     * @param filterTagSet  标签白名单的 ID 列表
     * @param filterTypeSet 种类白名单的编号列表
     * @param start         起始日期（包含）
     * @param end           结束日期（包含）
     * @param includeNoTag  是否包含无标签的流水记录
     * @param keyword       搜索关键词
     * @param db            数据库实例
     * @return 符合过滤条件的流水记录数据，支持响应式更新
     */
    public static Flowable<List<AccountUiModel>> loadAccountListDataFlowable(
            @NonNull Set<Long> filterTagSet,
            @NonNull Set<Integer> filterTypeSet,
            @Nullable LocalDate start,
            @Nullable LocalDate end,
            boolean includeNoTag,
            @Nullable String keyword,
            @NonNull BookkeepingDb db
    ) {
        //判断需要使用哪些过滤
        boolean useTypeFilter = !filterTypeSet.isEmpty();
        boolean useTagFilter = !filterTagSet.isEmpty();
        boolean useTimeFilter = start != null && end != null;
        boolean useSearchFilter = keyword != null && !keyword.isEmpty();

        //生成基础 SQL 语句
        StringBuilder sql = new StringBuilder(
                "SELECT * FROM accounts " +
                        "WHERE 1=1"
        );
        List<Object> args = new ArrayList<>();

        //种类过滤
        if (useTypeFilter) {
            sql.append(" AND ");
            sql.append("type IN (");

            int i = 0;
            for (Integer type : filterTypeSet) {
                sql.append("?");
                args.add(type);
                if (i < filterTagSet.size() - 1) sql.append(",");
                i++;
            }

            sql.append(")");
        }

        //标签过滤
        if (useTagFilter || includeNoTag) {
            sql.append(" AND (");

            //拼接标签过滤
            if (useTagFilter) {
                sql.append("accountId IN (SELECT accountId FROM accountTagRef WHERE tagId IN (");

                int i = 0;
                for (Long tagId : filterTagSet) {
                    sql.append("?");
                    args.add(tagId);
                    if (i < filterTagSet.size() - 1) sql.append(",");
                    i++;
                }

                sql.append("))");
            }

            //拼接连接词
            if (useTagFilter && includeNoTag) {
                sql.append(" OR ");
            }

            //添加无标签的筛选条件
            if (includeNoTag) {
                sql.append("accountId NOT IN (SELECT accountId FROM accountTagRef)");
            }

            sql.append(")");
        }

        //日期过滤
        if (useTimeFilter) {
            sql.append(" AND ");
            sql.append("dateTime >= ? AND dateTime < ?");
            args.add(DateTimeConverter.fromLocalDate(start));
            args.add(DateTimeConverter.fromLocalDate(end.plusDays(1)));
        }

        //搜索过滤
        if (useSearchFilter) {
            String safeKeyword = keyword.replace("/", "//").replace("%", "/%").replace("_", "/_");
            sql.append(" AND ");
            sql.append("remark LIKE ? ESCAPE '/'");
            args.add("%" + safeKeyword + "%");
        }

        //补上排序规则
        sql.append(" ORDER BY dateTime DESC");

        //执行查询并返回结果
        SimpleSQLiteQuery rawQuery = new SimpleSQLiteQuery(sql.toString(), args.toArray());
        AccountDao dao = db.accountDao();
        return dao.getAccountWithFilter(rawQuery)
                .map(accountList -> {
                    //按照日期分组
                    Map<LocalDate, List<AccountEntity>> dateGroupedMap = accountList.stream()
                            .collect(Collectors.groupingBy(
                                    accountEntity -> accountEntity.getDateTime().toLocalDate(),
                                    LinkedHashMap::new,
                                    Collectors.toList()
                            ));

                    //转换为 UiModel
                    List<AccountUiModel> resultList = new ArrayList<>();
                    for (Map.Entry<LocalDate, List<AccountEntity>> entry : dateGroupedMap.entrySet()) {
                        LocalDate date = entry.getKey();
                        String dateStr = date.format(CustomDateTimeFormatter.DATE_WITH_WEEK);
                        resultList.add(new AccountUiModel.Separator(dateStr));

                        List<AccountUiModel.Item> itemList = entry.getValue().stream()
                                .map(AccountUiModel.Item::new)
                                .collect(Collectors.toList());
                        resultList.addAll(itemList);
                    }

                    return resultList;
                });
    }

    /**
     * 插入新流水记录
     *
     * @param account         新流水记录
     * @param transfer        转账账户数据（仅当记录类型为转账时会写入）
     * @param mediaEntityList 位于永久目录下的媒体文件实体列表
     * @param tagIdList       与该记录绑定的标签的 ID 列表
     * @param context         上下文
     * @return 新添加的流水记录的编号
     */
    public static Single<Long> addNewAccount(
            AccountEntity account,
            AccountTransferEntity transfer,
            @Nullable List<MediaEntity> mediaEntityList,
            @Nullable List<Long> tagIdList,
            Context context
    ) {
        return Single.defer(() -> {
            BookkeepingDb db = BookkeepingDb.getInstance(context);
            long accountId = db.accountDao().addAccount(account, transfer, mediaEntityList, tagIdList);

            //检查预算
            BudgetService.checkAndSendLowBalanceNotification(context);

            return Single.just(accountId);
        });
    }

    /**
     * 修改流水记录
     *
     * @param account         修改后的流水记录
     * @param transfer        转账账户数据（仅当记录类型为转账时会写入）
     * @param mediaEntityList 位于永久目录下的媒体文件实体列表，可能包含新添加的媒体
     * @param tagIdList       与该记录绑定的标签的 Id 列表
     * @param context         上下文
     * @return 是否完成
     */
    public static Completable modifyAccount(
            AccountEntity account,
            AccountTransferEntity transfer,
            List<MediaEntity> mediaEntityList,
            List<Long> tagIdList,
            Context context
    ) {
        return Completable.defer(() -> {
            BookkeepingDb db = BookkeepingDb.getInstance(context);
            Set<Uri> oldMediaUriSet = db.accountDao().modifyAccount(account, transfer, mediaEntityList, tagIdList);

            //删除旧媒体文件
            if (oldMediaUriSet != null) {
                for (Uri uri : oldMediaUriSet) {
                    FileHelper.deleteFile(uri, context);
                }
            }

            //检查预算
            BudgetService.checkAndSendLowBalanceNotification(context);

            return Completable.complete();
        });
    }

    /**
     * 删除流水记录
     *
     * @param account 需要删除的流水记录
     * @param context 上下文
     * @return 是否完成
     */
    public static Completable deleteAccount(AccountEntity account, Context context) {
        return Completable.defer(() -> {
            BookkeepingDb db = BookkeepingDb.getInstance(context);
            Set<Uri> oldMediaUriSet = db.accountDao().removeAccount(account);

            //移除媒体文件
            if (oldMediaUriSet != null) {
                for (Uri uri : oldMediaUriSet) {
                    FileHelper.deleteFile(uri, context);
                }
            }

            //检查预算
            BudgetService.checkAndSendLowBalanceNotification(context);

            return Completable.complete();
        });
    }
}
