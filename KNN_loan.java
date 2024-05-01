import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;
import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import javax.swing.*;
import java.awt.*;

public class KNN_loan {

    // Define a class to represent a loan applicant
    public static class LoanApplicant {
        double loanAmount;
        double loanAmountTerm;
        double creditHistory;
        String loanStatus;

        public LoanApplicant(double loanAmount, double loanAmountTerm, double creditHistory, String loanStatus) {
            this.loanAmount = loanAmount;
            this.loanAmountTerm = loanAmountTerm;
            this.creditHistory = creditHistory;
            this.loanStatus = loanStatus;
        }
    }

    // Method to load data from CSV file starting from line 2
    public static List<LoanApplicant> loadData(String filename) throws FileNotFoundException {
        List<LoanApplicant> data = new ArrayList<>();
        Scanner scanner = new Scanner(new File(filename));
        // Skip the first line (header)
        if (scanner.hasNextLine()) {
            scanner.nextLine();
        }
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            String[] parts = line.split(",");
            if (parts.length >= 13) { // Ensure that there are enough fields in the line
                String loanAmountStr = parts[8].trim(); // Index of LoanAmount
                String loanAmountTermStr = parts[9].trim(); // Index of Loan_Amount_Term
                String creditHistoryStr = parts[10].trim(); // Index of Credit_History
                String loanStatus = parts[12].trim(); // Index of Loan_Status
                
                // Check if any of the fields are empty
                if (!loanAmountStr.isEmpty() && !loanAmountTermStr.isEmpty() && !creditHistoryStr.isEmpty()) {
                    double loanAmountParsed = Double.parseDouble(loanAmountStr);
                    double loanAmountTermParsed = Double.parseDouble(loanAmountTermStr);
                    double creditHistoryParsed = Double.parseDouble(creditHistoryStr);
                    data.add(new LoanApplicant(loanAmountParsed, loanAmountTermParsed, creditHistoryParsed, loanStatus));
                }
            }
        }
        scanner.close();
        return data;
    }

    // Method to normalize data
    public static void normalizeData(List<LoanApplicant> data) {
        // Find min and max values for each feature
        double maxLoanAmount = Double.MIN_VALUE;
        double minLoanAmount = Double.MAX_VALUE;
        double maxLoanAmountTerm = Double.MIN_VALUE;
        double minLoanAmountTerm = Double.MAX_VALUE;
        double maxCreditHistory = Double.MIN_VALUE;
        double minCreditHistory = Double.MAX_VALUE;

        for (LoanApplicant applicant : data) {
            maxLoanAmount = Math.max(maxLoanAmount, applicant.loanAmount);
            minLoanAmount = Math.min(minLoanAmount, applicant.loanAmount);
            maxLoanAmountTerm = Math.max(maxLoanAmountTerm, applicant.loanAmountTerm);
            minLoanAmountTerm = Math.min(minLoanAmountTerm, applicant.loanAmountTerm);
            maxCreditHistory = Math.max(maxCreditHistory, applicant.creditHistory);
            minCreditHistory = Math.min(minCreditHistory, applicant.creditHistory);
        }

        // Normalize each feature
        for (LoanApplicant applicant : data) {
            applicant.loanAmount = (applicant.loanAmount - minLoanAmount) / (maxLoanAmount - minLoanAmount);
            applicant.loanAmountTerm = (applicant.loanAmountTerm - minLoanAmountTerm) / (maxLoanAmountTerm - minLoanAmountTerm);
            applicant.creditHistory = (applicant.creditHistory - minCreditHistory) / (maxCreditHistory - minCreditHistory);
        }
    }

    // Method to split data into train and test sets
    public static void splitData(List<LoanApplicant> data, List<LoanApplicant> trainSet, List<LoanApplicant> testSet, double splitRatio) {
        Random random = new Random();
        for (LoanApplicant applicant : data) {
            if (random.nextDouble() < splitRatio) {
                trainSet.add(applicant);
            } else {
                testSet.add(applicant);
            }
        }
    }

    // Method to implement KNN algorithm
    public static String knn(List<LoanApplicant> trainSet, LoanApplicant testInstance, int k) {
        // Calculate distances to all data points in train set
        Map<Double, LoanApplicant> distances = new HashMap<>();
        for (LoanApplicant trainInstance : trainSet) {
            double distance = Math.sqrt(Math.pow(testInstance.loanAmount - trainInstance.loanAmount, 2) +
                                        Math.pow(testInstance.loanAmountTerm - trainInstance.loanAmountTerm, 2) +
                                        Math.pow(testInstance.creditHistory - trainInstance.creditHistory, 2));
            distances.put(distance, trainInstance);
        }

        // Sort distances and get top k neighbors
        List<LoanApplicant> nearestNeighbors = new ArrayList<>();
        distances.keySet().stream().sorted().limit(k).forEach(distance -> nearestNeighbors.add(distances.get(distance)));

        // Count the votes for each class
        int yesCount = 0;
        int noCount = 0;
        for (LoanApplicant neighbor : nearestNeighbors) {
            if (neighbor.loanStatus.equalsIgnoreCase("Y")) {
                yesCount++;
            } else {
                noCount++;
            }
        }

        // Make prediction based on majority vote
        if (yesCount > noCount) {
            return "Y";
        } else {
            return "N";
        }
    }

    // Method to calculate precision
    public static double calculatePrecision(List<String> actual, List<String> predicted, String positiveClass) {
        int truePositives = 0;
        int falsePositives = 0;
        for (int i = 0; i < actual.size(); i++) {
            if (predicted.get(i).equalsIgnoreCase(positiveClass)) {
                if (actual.get(i).equalsIgnoreCase(predicted.get(i))) {
                    truePositives++;
                } else {
                    falsePositives++;
                }
            }
        }
        if (truePositives + falsePositives == 0) {
            return 0.0;
        }
        return (double) truePositives / (truePositives + falsePositives);
    }

    // Method to calculate R-squared score
    public static double calculateRSquared(List<Double> actual, List<Double> predicted) {
        double sumSquaredErrors = 0.0;
        double mean = 0.0;
        for (double value : actual) {
            mean += value;
        }
        mean /= actual.size();
        for (int i = 0; i < actual.size(); i++) {
            double error = actual.get(i) - predicted.get(i);
            sumSquaredErrors += Math.pow(error, 2);
        }
        double sumSquaredDeviations = 0.0;
        for (double value : actual) {
            sumSquaredDeviations += Math.pow(value - mean, 2);
        }
        if (sumSquaredDeviations == 0.0) {
            return 1.0; // R-squared score is 1.0 if there is no variance in the data
        }
        return 1.0 - (sumSquaredErrors / sumSquaredDeviations);
    }

    // Method to calculate Mean Squared Error (MSE)
    public static double calculateMSE(List<Double> actual, List<Double> predicted) {
        double sumSquaredErrors = 0.0;
        for (int i = 0; i < actual.size(); i++) {
            double error = actual.get(i) - predicted.get(i);
            sumSquaredErrors += Math.pow(error, 2);
        }
        return sumSquaredErrors / actual.size();
    }

    // Method to calculate Mean Absolute Error (MAE)
    public static double calculateMAE(List<Double> actual, List<Double> predicted) {
        double sumAbsoluteErrors = 0.0;
        for (int i = 0; i < actual.size(); i++) {
            sumAbsoluteErrors += Math.abs(actual.get(i) - predicted.get(i));
        }
        return sumAbsoluteErrors / actual.size();
    }
    public static void createScatterPlot(List<Double> actualLabels, List<Double> predictedLabels) {
        XYSeries series = new XYSeries("Actual vs. Predicted");
        for (int i = 0; i < actualLabels.size(); i++) {
            series.add(actualLabels.get(i), predictedLabels.get(i));
        }

        XYSeriesCollection dataset = new XYSeriesCollection(series);
        JFreeChart chart = ChartFactory.createScatterPlot(
                "Actual vs. Predicted", "Actual", "Predicted", dataset);
        
        // Display the chart in a frame
        ChartPanel chartPanel = new ChartPanel(chart);
        JFrame frame = new JFrame("Scatter Plot");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.add(chartPanel, BorderLayout.CENTER);
        frame.pack();
        frame.setVisible(true);
    }

    // Main method
    public static void main(String[] args) throws IOException {
        // Load data
        List<LoanApplicant> loanData = loadData("C:/Users/Admin/OneDrive/Documents/ML/lab4/lab4/loan_data.csv");

        // Preprocess data
        normalizeData(loanData);

        // Split data into train and test sets
        List<LoanApplicant> trainSet = new ArrayList<>();
        List<LoanApplicant> testSet = new ArrayList<>();
        double splitRatio = 0.8;
        splitData(loanData, trainSet, testSet, splitRatio);

        // Test the KNN algorithm on test set
        int k = 5; // Number of neighbors to consider
        List<String> actualLabels = new ArrayList<>();
        List<String> predictedLabels = new ArrayList<>();
        for (LoanApplicant testInstance : testSet) {
            String actualClass = testInstance.loanStatus;
            String predictedClass = knn(trainSet, testInstance, k);
            actualLabels.add(actualClass);
            predictedLabels.add(predictedClass);
        }

        // Convert loan statuses to numerical values
        List<Double> actualLabelsNumeric = new ArrayList<>();
        List<Double> predictedLabelsNumeric = new ArrayList<>();
        for (String status : actualLabels) {
            actualLabelsNumeric.add(status.equalsIgnoreCase("Y") ? 1.0 : 0.0);
        }
        for (String status : predictedLabels) {
            predictedLabelsNumeric.add(status.equalsIgnoreCase("Y") ? 1.0 : 0.0);
        }

        // Calculate metrics
        double accuracy = calculatePrecision(actualLabels, predictedLabels, "Y");
        double rSquared = calculateRSquared(actualLabelsNumeric, predictedLabelsNumeric);
        double mse = calculateMSE(actualLabelsNumeric, predictedLabelsNumeric);
        double mae = calculateMAE(actualLabelsNumeric, predictedLabelsNumeric);

        // Output metrics
        System.out.println("Accuracy: " + accuracy);
        System.out.println("R-squared: " + rSquared);
        System.out.println("Mean Squared Error (MSE): " + mse);
        System.out.println("Mean Absolute Error (MAE): " + mae);
    }
}
