package com.sly.coffer.ui.pages.main.home;

import androidx.core.util.Pair;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class HomePageViewModel extends ViewModel {
    private final MutableLiveData<Double> reportBalanceLiveData = new MutableLiveData<>();
    private final MutableLiveData<Pair<Double, Double>> reportExpenseIncomeLiveData = new MutableLiveData<>();
    private final MutableLiveData<Double> loanBorrowedLiveData = new MutableLiveData<>();
    private final MutableLiveData<Double> loanLentLiveData = new MutableLiveData<>();

    public void updateBalance(double balance) {
        reportBalanceLiveData.setValue(balance);
    }

    public void updateExpenseIncome(Pair<Double, Double> pair) {
        reportExpenseIncomeLiveData.setValue(pair);
    }

    public void updateBorrowed(double borrowed) {
        loanBorrowedLiveData.setValue(borrowed);
    }

    public void updateLent(double lent) {
        loanLentLiveData.setValue(lent);
    }

    public MutableLiveData<Double> getReportBalanceLiveData() {
        return reportBalanceLiveData;
    }

    public MutableLiveData<Pair<Double, Double>> getReportExpenseIncomeLiveData() {
        return reportExpenseIncomeLiveData;
    }

    public MutableLiveData<Double> getLoanBorrowedLiveData() {
        return loanBorrowedLiveData;
    }

    public MutableLiveData<Double> getLoanLentLiveData() {
        return loanLentLiveData;
    }
}
