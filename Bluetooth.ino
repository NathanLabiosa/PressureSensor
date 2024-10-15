#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEServer.h>
#include <BLE2902.h>

#define SERVICE_UUID        "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
#define CHARACTERISTIC_UUID "beb5483e-36e1-4688-b7f5-ea07361b26a8"

BLEServer *pServer = nullptr;
BLECharacteristic *pCharacteristic = nullptr;

void setup() {
  Serial.begin(115200); // Start serial communication
  BLEDevice::init("ESP32 Voltage Meter"); // Initialize BLE device

  pServer = BLEDevice::createServer(); // Create a BLE server
  BLEService *pService = pServer->createService(SERVICE_UUID); // Create a BLE service

  // Create a BLE characteristic
  pCharacteristic = pService->createCharacteristic(
                      CHARACTERISTIC_UUID,
                      BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_NOTIFY
                    );
  pCharacteristic->addDescriptor(new BLE2902());

  pService->start(); // Start the service
  pServer->getAdvertising()->start(); // Start advertising
  Serial.println("Waiting for a client connection to notify...");

  pinMode(36, INPUT); // Setup ADC0 pin
}

void loop() {
  int rawValue = analogRead(36); // Read the analog value from ADC0
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
