package com.sly.coffer.data.save.db.services;

import androidx.annotation.NonNull;

import com.sly.coffer.data.save.db.BookkeepingDb;
import com.sly.coffer.data.save.db.daos.ExtraDataDao;
import com.sly.coffer.data.save.db.entities.composite.union.AccountLoanUnionModel;

import java.util.List;

import io.reactivex.rxjava3.core.Flowable;

public class ExtraDataService {
    /**
     * 获取带有借款记录的流水记录
     *
     * @param db      数据库实例
     * @param keyword 搜索关键词
     * @return 符合搜索条件的带有借款记录的流水记录数据
     */
    public static Flowable<List<AccountLoanUnionModel>> getAccountWithLoanFlowable(
            @NonNull BookkeepingDb db,
            String keyword
    ) {
        ExtraDataDao dao = db.extraDataDao();
        String safeKeyword = "";

        if (keyword != null && !keyword.trim().isEmpty()) {
            safeKeyword = keyword.replace("/", "//")
                    .replace("%", "/%")
                    .replace("_", "/_");
        }

        return dao.getAccountWithLoanFlowable(safeKeyword);
    }
}
