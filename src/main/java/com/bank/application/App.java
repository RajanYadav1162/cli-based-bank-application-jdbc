package com.bank.application;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;

import com.mysql.cj.protocol.Resultset;

class DatabaseConnection {
	Connection connection;

	public DatabaseConnection(String url, String username, String password) throws SQLException {
		connection = DriverManager.getConnection(url, username, password);
		if (connection != null) {
			System.out.println("database connected! ");
		}
	}

	public boolean createAccount(SignUpData data) throws SQLException {
		PreparedStatement statement = connection.prepareStatement(
				"insert into customers (accountNumber, name, password) values (?, ?, ?)",
				Statement.RETURN_GENERATED_KEYS);

		String accountNumber = "";
		char[] array = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

		for (int i = 1; i <= 5; i++) {
			int randomIndx = (int) (Math.random() * 52);
			accountNumber += array[randomIndx];
		}
		statement.setString(1, accountNumber);
		statement.setString(2, data.name);
		statement.setString(3, data.password);

		try {
			statement.executeUpdate();
			int id = 0;
			ResultSet rsKey = statement.getGeneratedKeys();
			if (rsKey.next()) {
				id = rsKey.getInt(1);
			}
			// after creating user account, i will also initialise users defult acc in bank
			// with bal 0;
			statement = connection.prepareStatement("insert into bank (customerID, balance) values (?, ?)");
			statement.setString(1, id + "");
			statement.setString(2, 0 + "");
			statement.executeUpdate();
			return true;
		} catch (Exception err) {
			System.out.println(err);
		}
		return false;

	}

	public boolean loginAccount(LoginData data) throws SQLException {
		PreparedStatement statement = connection.prepareStatement("select * from customers where accountNumber =?");
		statement.setString(1, data.accountNumber);
		ResultSet rs = statement.executeQuery();
		if (!((Resultset) rs).hasRows()) {
			return false;
		}
		if (rs.next()) {
			if (data.password.equals(rs.getString("password"))) {
				App.isAuthenticated = true;
				App.customerID = rs.getString("id");
				App.customerName = rs.getString("name");
				return true;
			} else {
				return false;
			}
		}
		return false;

	}

	public String getBalance() throws SQLException {
		PreparedStatement statement = connection.prepareStatement("select balance from bank where customerID=?");
		statement.setString(1, App.customerID);
		ResultSet rs = statement.executeQuery();
		if (rs.next()) {
			double balance = rs.getDouble("balance");
			return balance + "";
		} else {
			System.out.println("some error occurs");
		}
		return null;
	}

	public boolean depositBalance(double amount) throws SQLException {

		PreparedStatement statement = connection.prepareStatement("update bank set balance =? where customerID =? ");

		// get old balance
		PreparedStatement statementForOldStatement = connection
				.prepareStatement("select balance from bank where customerID=?");
		statementForOldStatement.setString(1, App.customerID);

		ResultSet rs = statementForOldStatement.executeQuery();
		double oldBal = 0;
		if (rs.next()) {
			oldBal = rs.getDouble("balance");
		}

		statement.setDouble(1, oldBal + amount);
		statement.setString(2, App.customerID);
		statement.executeUpdate();

		return true;
	}

	public boolean withDraw(double amount) throws SQLException {

		PreparedStatement statement = connection.prepareStatement("update bank set balance =? where customerID =? ");

		PreparedStatement statementForOldStatement = connection
				.prepareStatement("select balance from bank where customerID=?");
		statementForOldStatement.setString(1, App.customerID);

		ResultSet rs = statementForOldStatement.executeQuery();
		double oldBal = 0;
		if (rs.next()) {
			oldBal = rs.getDouble("balance");
		}

		if (oldBal < amount) {
			return false;
		}

		statement.setDouble(1, oldBal - amount);
		statement.setString(2, App.customerID);
		statement.executeUpdate();
		return true;
	}

}

class Customer {
	int id;
	String accoutNumber;
	String name;
	String password;

	public Customer(int id, String accoutNumber, String name, String password) {

		this.id = id;
		this.accoutNumber = accoutNumber;
		this.name = name;
		this.password = password;
	}

}

class Bank {
	int id;
	double balance;
	int customerAccountNumber;

	public Bank(int id, double balance, int customerAccountNumber) {
		this.id = id;
		this.balance = balance;
		this.customerAccountNumber = customerAccountNumber;
	}

}

class SignUpData {
	String name;
	String password;

	public SignUpData(String name, String password) {
		// TODO Auto-generated constructor stub
		this.name = name;
		this.password = password;
	}
}

class LoginData {
	String accountNumber;
	String password;

	public LoginData(String accoutString, String password) {
		// TODO Auto-generated constructor stub
		this.accountNumber = accoutString;
		this.password = password;
	}
}

class AuthView {
	public static Scanner sc = new Scanner(System.in);

	public static void menu() {
		System.out.println("1 to signin");
		System.out.println("2 to signup");
		System.out.println("3 to exit");
	}

	public static int getChoice() {
		int choice;
		System.out.println("enter your choice");
		choice = sc.nextInt();
		return choice;
	}

	public static SignUpData signUpData() {

		String password;
		String name;
		System.out.println("enter your name");
		name = sc.next();
		System.out.println("enter your password");
		password = sc.next();
		return new SignUpData(name, password);

	}

	public static LoginData loginData() {

		String password;
		String accountNumber;
		System.out.println("enter your account number");
		accountNumber = sc.next();
		System.out.println("enter your password");
		password = sc.next();
		return new LoginData(accountNumber, password);
	}
}

class BankView {

	public static Scanner sc = new Scanner(System.in);

	public static void menu() {
		System.out.println("1 to get balance");
		System.out.println("2 to deposit");
		System.out.println("3 to withdraw");
		System.out.println("4 to exit");

	}

	public static int getChoice() {
		int choice;
		System.out.println("enter your choice");
		choice = sc.nextInt();
		return choice;
	}

	public static double getAmount() {
		System.out.println("enter amount ");
		double amount = sc.nextDouble();
		return amount;
	}
}

public class App {

	// global variables so that we can use it everywhere in our application
	public static boolean isAuthenticated = false;
	public static String customerID = "";
	public static String customerName = "";

	public static void main(String[] args) throws SQLException {
		DatabaseConnection connection = new DatabaseConnection("jdbc:mysql://localhost:3306/BankDB", "root", "");

		// if not authenticated, show the auth menu;
		while (!isAuthenticated) {
			AuthView.menu();
			int choice = AuthView.getChoice();
			if (choice == 1) {
				var data = AuthView.loginData();
				var res = connection.loginAccount(data);

				if (res) {
					System.out.println("login successfull");
					System.out.println("welcome " + customerName);

				} else {
					System.out.println("invalid credentials");
				}
			} else if (choice == 2) {
				var data = AuthView.signUpData();
				var res = connection.createAccount(data);
				if (res) {
					System.out.println("successful");
				} else {
					System.out.println("error occurs");
				}

			} else {
				return;
			}
		}

		// if authenticated show bank menu;
		while (isAuthenticated) {
			BankView.menu();
			int choice = BankView.getChoice();
			if (choice == 1) {
				var bal = connection.getBalance();
				System.out.println("balance is " + bal);
			}

			else if (choice == 2) {
				double amount = BankView.getAmount();
				var res = connection.depositBalance(amount);
				if (res) {
					System.out.println("successfully deposited");
				} else {
					System.out.println("something went wrong");
				}
			} else if (choice == 3) {
				double amount = BankView.getAmount();
				var res = connection.withDraw(amount);
				if (res) {
					System.out.println("successfully withdrawn");
				} else {
					System.out.println("something went wrong");
				}
			} else
				return;
		}

	}
}
