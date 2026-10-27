package com.sly.coffer.helpers.compute;

import androidx.annotation.Nullable;

import com.sly.coffer.auxiliary.enums.types.InterestPeriodType;
import com.sly.coffer.data.save.db.entities.AccountEntity;
import com.sly.coffer.data.save.db.entities.ExtraLoanEntity;
import com.sly.coffer.data.save.db.entities.composite.union.AccountLoanUnionModel;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DebtHelper {
    /**
     * 计算欠款
     *
     * @param modelList 带有借款记录和还款记录的数据模型
     * @return 该模型剩下的欠款，若无法计算则返回 null
     */
    public static double computeDebt(List<AccountLoanUnionModel> modelList) {
        if (modelList == null || modelList.isEmpty()) return 0;

        double total = 0;
        for (AccountLoanUnionModel model : modelList) {
            Double debt = computeDebt(model);
            if (debt != null) {
                total += debt;
            }
        }

        return total;
    }

    /**
     * 计算欠款
     *
     * @param model 带有借款记录和还款记录的数据模型
     * @return 该模型剩下的欠款，若无法计算则返回 null
     */
    @Nullable
    public static Double computeDebt(AccountLoanUnionModel model) {
        if (model == null || model.getLoan() == null) return null;

        //读取数据
        ExtraLoanEntity loan = model.getLoan();
        AccountEntity loanAccount = model.getAccount();
        double principal = loanAccount.getAmount();
        int interestPeriodOrdinal = loan.getInterestPeriodType();
        boolean allowCompoundInterest = loan.isAllowCompoundInterest();
        int yearBasepoint = loan.getYearBasepoint();

        //计算利息
        double debt;
        LocalDate lastSettleDate = loanAccount.getDateTime().toLocalDate();
        if (allowCompoundInterest) {
            if (interestPeriodOrdinal == InterestPeriodType.EVERY_DAY.ordinal()) {
                debt = principal;

                //将还款记录根据日期分组
                Map<LocalDate, List<AccountEntity>> repayAccountMap = model.getRepaymentList().stream()
                        .collect(Collectors.groupingBy(
                                account -> account.getDateTime().toLocalDate(),
                                HashMap::new,
                                Collectors.toList()
                        ));
                List<LocalDate> repayDateList = repayAccountMap.keySet().stream()
                        .sorted()
                        .collect(Collectors.toList());

                //每次还款时抵消利息
                for (LocalDate repayDate : repayDateList) {
                    long dayDifference = ChronoUnit.DAYS.between(lastSettleDate, repayDate);
                    lastSettleDate = repayDate;

                    double compounded = getSumPrincipalAndCompoundInterest(
                            debt, yearBasepoint / 360.0, dayDifference
                    );
                    List<AccountEntity> repaymentList = repayAccountMap.get(repayDate);
                    double repayment;
                    if (repaymentList != null) {
                        repayment = repaymentList.stream()
                                .mapToDouble(AccountEntity::getAmount)
                                .sum();
                    } else {
                        repayment = 0;
                    }

                    //将未抵消完的利息计入本金
                    debt = compounded - repayment;
                }

                //计算到目前为止的利息
                long dayDifference = ChronoUnit.DAYS.between(lastSettleDate, LocalDate.now());
                debt = getSumPrincipalAndCompoundInterest(
                        debt, yearBasepoint / 360.0, dayDifference
                );
            } else {
                //判断结息周期
                debt = principal;
                Function<LocalDate, LocalDate> forwardFunction;
                Function<Integer, Double> periodInterestRateCalculator;
                if (interestPeriodOrdinal == InterestPeriodType.EVERY_MONTH.ordinal()) {
                    forwardFunction = date -> date.plusMonths(1);
                    periodInterestRateCalculator = basepoint -> basepoint / (10000.0 * 12);
                } else if (interestPeriodOrdinal == InterestPeriodType.EVERY_QUARTER.ordinal()) {
                    forwardFunction = date -> date.plusMonths(3);
                    periodInterestRateCalculator = basepoint -> basepoint / (10000.0 * 4);
                } else if (interestPeriodOrdinal == InterestPeriodType.EVERY_YEAR.ordinal()) {
                    forwardFunction = date -> date.plusYears(1);
                    periodInterestRateCalculator = basepoint -> basepoint / 10000.0;
                } else {
                    return null;
                }

                //逐个结息周期计算复利
                while (!forwardFunction.apply(lastSettleDate).isAfter(LocalDate.now())) {
                    LocalDate currentPeriod = lastSettleDate;
                    LocalDate nextPeriod = forwardFunction.apply(lastSettleDate);

                    //获取在日期范围内的还款记录
                    List<AccountEntity> yearPeriodRepayAccountList = model.getRepaymentList().stream()
                            .filter(account ->
                                    account.getDateTime().isAfter(currentPeriod.atStartOfDay()) &&
                                            !account.getDateTime().isAfter(nextPeriod.atStartOfDay()))
                            .collect(Collectors.toList());
                    lastSettleDate = nextPeriod;

                    //抵消利息，并将剩余利息计入本金
                    double interest = debt * periodInterestRateCalculator.apply(yearBasepoint);
                    double repayment = yearPeriodRepayAccountList.stream()
                            .mapToDouble(AccountEntity::getAmount)
                            .sum();
                    debt = debt + interest - repayment;
                }

                //计算零头日期的日利息
                long dayDifference = ChronoUnit.DAYS.between(lastSettleDate, LocalDate.now());
                double compounded = getSumPrincipalAndCompoundInterest(
                        debt, yearBasepoint / 360.0, dayDifference
                );
                LocalDate currentYear = lastSettleDate;
                LocalDate nextYear = lastSettleDate.plusYears(1);
                List<AccountEntity> leftAccountList = model.getRepaymentList().stream()
                        .filter(account ->
                                account.getDateTime().isAfter(currentYear.atStartOfDay()) &&
                                        !account.getDateTime().isAfter(nextYear.atStartOfDay()))
                        .collect(Collectors.toList());
                double repayment = leftAccountList.stream()
                        .mapToDouble(AccountEntity::getAmount)
                        .sum();
                debt = compounded - repayment;
            }
        } else {
            debt = principal;
            double acInterest = 0;  //累计的没有抵消完的利息

            //将还款记录根据日期分组
            Map<LocalDate, List<AccountEntity>> repayAccountMap = model.getRepaymentList().stream()
                    .collect(Collectors.groupingBy(
                            account -> account.getDateTime().toLocalDate(),
                            HashMap::new,
                            Collectors.toList()
                    ));
            List<LocalDate> repayDateList = repayAccountMap.keySet().stream()
                    .sorted()
                    .collect(Collectors.toList());

            //每次还款时抵消利息，多余的还款再抵消本金
            for (LocalDate repayDate : repayDateList) {
                long dayDifference = ChronoUnit.DAYS.between(lastSettleDate, repayDate);
                lastSettleDate = repayDate;

                //计算利息和还款
                double interest = debt * dayDifference * yearBasepoint / 360 / 10000;
                List<AccountEntity> repaymentList = repayAccountMap.get(repayDate);
                double repayment;
                if (repaymentList != null) {
                    repayment = repaymentList.stream()
                            .mapToDouble(AccountEntity::getAmount)
                            .sum();
                } else {
                    repayment = 0;
                }

                //抵消利息/累加利息
                if (repayment > interest + acInterest) {
                    debt -= repayment - interest - acInterest;
                    acInterest = 0;
                } else {
                    acInterest += interest - repayment;
                }
            }

            //计算最后一次还款到现在累计的本息和
            long dayDifference = ChronoUnit.DAYS.between(lastSettleDate, LocalDate.now());
            double interest = debt * dayDifference * yearBasepoint / 360 / 10000;
            debt += interest + acInterest;
        }

        return Math.max(debt, 0);
    }

    /**
     * 计算本金与复利之和
     *
     * @param principal     本金
     * @param rateBasepoint 利率的基点（需要除以10000转换得到利率）
     * @param cycle         利息结算次数
     * @return 本金与复利之和
     */
    private static double getSumPrincipalAndCompoundInterest(double principal, double rateBasepoint, long cycle) {
        double sum = principal;
        for (int i = 0; i < cycle; i++) {
            double interest = sum * rateBasepoint / 10000;
            sum += interest;
        }
        return sum;
    }
}
