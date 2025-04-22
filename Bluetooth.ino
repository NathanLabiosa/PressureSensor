#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEServer.h>
#include <BLE2902.h>
#include <UMS3.h>

#define SERVICE_UUID        "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
#define CHARACTERISTIC_UUID "beb5483e-36e1-4688-b7f5-ea07361b26a8"

UMS3 ums3;
BLEServer *pServer = nullptr;
BLECharacteristic *pCharacteristic = nullptr;

// this flag tells us whether the last write was a battery request
volatile bool batteryRequested = false;

class MyCallbacks : public BLECharacteristicCallbacks {
  void onWrite(BLECharacteristic* chr) override {
    // any write means “send me battery voltage now”
    batteryRequested = true;
  }
};

void setup() {
  Serial.begin(115200); // Start serial communication
  ums3.begin(); 
  BLEDevice::init("ESP32 Voltage Meter"); // Initialize BLE device

  pServer = BLEDevice::createServer(); // Create a BLE server
  BLEService *pService = pServer->createService(SERVICE_UUID); // Create a BLE service

  // Create a BLE characteristic
  pCharacteristic = pService->createCharacteristic(
                      CHARACTERISTIC_UUID,
                      BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_NOTIFY | BLECharacteristic::PROPERTY_WRITE
                    );
  pCharacteristic->addDescriptor(new BLE2902());
  pCharacteristic->setCallbacks(new MyCallbacks());

  pService->start(); // Start the service
  pServer->getAdvertising()->start(); // Start advertising
  
  pinMode(9, INPUT); // Setup ADC0 pin
}

void loop() {

  if (batteryRequested) {
    batteryRequested = false;
    float batt = ums3.getBatteryVoltage();
    char buf[20];
    sprintf(buf, "BAT:%.2f V", batt);
    pCharacteristic->setValue((uint8_t*)buf, strlen(buf));
    pCharacteristic->notify();
    delay(200);       
  }

  int rawValue = analogRead(9); // Read the analog value from ADC0
  float voltage = rawValue * (3.3 / 4095.0); // Convert that value to voltage
  char valueStr[20]; // Buffer to hold the voltage string
  sprintf(valueStr, "%.2f V", voltage); // Format the voltage into a string
  pCharacteristic->setValue((uint8_t*)valueStr, strlen(valueStr)); // Update BLE characteristic
  pCharacteristic->notify(); // Notify any connected clients
  
  Serial.print("ADC Value: "); // Print the raw ADC value
  Serial.print(rawValue);
  Serial.print(" - Voltage: "); // Print the formatted voltage
  Serial.println(valueStr);

  delay(1000); // Delay for 1 second before the next read
}
