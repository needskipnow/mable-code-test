package com.mable.codetest;


public class BankApp {

    public static void main(String[] args) {
        new BatchProcessor("mable_acc_balance.csv", "mable_trans.csv", "reports").process();
    }
}
