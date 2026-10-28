package com.example.myapp.domain.model;

/**
 * Model chứa các thông số cảm biến nông nghiệp.
 */
public class SensorData {
    private double temperature;  // Nhiệt độ (°C)
    private double humidity;     // Độ ẩm (%)
    private double phValue;      // Độ pH
    private double nitrogen;     // N (Nitơ) - mg/kg
    private double phosphorus;   // P (Photpho) - mg/kg
    private double potassium;    // K (Kali) - mg/kg
    private double waterLevel;   // Mực nước (cm)
    private double ecValue;      // Độ mặn/Dẫn điện EC (mS/cm)

    public SensorData(double temperature, double humidity, double phValue, 
                      double nitrogen, double phosphorus, double potassium, 
                      double waterLevel, double ecValue) {
        this.temperature = temperature;
        this.humidity = humidity;
        this.phValue = phValue;
        this.nitrogen = nitrogen;
        this.phosphorus = phosphorus;
        this.potassium = potassium;
        this.waterLevel = waterLevel;
        this.ecValue = ecValue;
    }

    // Getters
    public double getTemperature() { return temperature; }
    public double getHumidity() { return humidity; }
    public double getPhValue() { return phValue; }
    public double getNitrogen() { return nitrogen; }
    public double getPhosphorus() { return phosphorus; }
    public double getPotassium() { return potassium; }
    public double getWaterLevel() { return waterLevel; }
    public double getEcValue() { return ecValue; }
}