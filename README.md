### Mable coding test implemented in Java 17
modelling end-of-day settlement between client's accounts

### BatchProcessor Notes
- loads account balances and transfer requests CSV files
- performs transfer when source account balance has sufficient funds
- prints updated account balances at the end of the batch run into mable_acc_balance_post_run_yyyy-MM-dd'T'hh-mm-ss.csv
- prints individual transaction details and status into mable_transfer_run_yyyy-MM-dd'T'hh-mm-ss.csv

#### Run the app in IDE:
run BankApp.java

#### Build and execute test(s)
mvn clean install
