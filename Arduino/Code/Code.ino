#include <SoftwareSerial.h>
SoftwareSerial hc06(0, 1);
#define ABSZERO 273.15
#define MAXANALOGREAD 1023.0
#define ANALOGPIN A1
#define RELAY A2

float temperature_NTC(float T0, float R0, float T1, float R1, float RV, float VA_VB)
{
  T0 += ABSZERO;
  T1 += ABSZERO;
  float B = (T0 * T1) / (T1 - T0) * log(R0 / R1);
  float RN = RV * VA_VB / (1 - VA_VB);
  return T0 * B / (B + T0 * log(RN / R0)) - ABSZERO;
}

float T0 = 25.0;
float R0 = 10000.0;
float T1 = 100.0;
float R1 = 1000.0;
float Vorwiderstand = 10000.0;

int aValue;
float temp;
int temperature = 0;
bool control = true;
String cmd;
int status;

void setup()
{
  hc06.begin(9600);
  Serial.begin(9600);
  status = 0;
  pinMode(RELAY, OUTPUT);
  digitalWrite(RELAY, HIGH);
}

void loop()
{
  aValue = analogRead(ANALOGPIN);
  temp = temperature_NTC(T0, R0, T1, R1, Vorwiderstand, aValue / MAXANALOGREAD);
  cmd = "";
  Serial.print('#');
  Serial.print(temp);
  Serial.print('+');
  if (temperature != 0)
  {
    Serial.print(temperature);
  }
  else
  {
    Serial.print("20");
  }
  Serial.print('+');
  Serial.print(status);
  Serial.println('~');
  while (hc06.available() > 0)
  {
    cmd += (char)hc06.read();
  }
  if (cmd != "")
  {
    if (cmd == "N")
    {
      control = false;
      status = 1;
      temperature = 0;
      digitalWrite(RELAY, LOW);
    }
    else if (cmd == "F")
    {
      control = false;
      status = 0;
      temperature = 0;
      digitalWrite(RELAY, HIGH);
    }
    else
    {
      temperature = cmd.toInt();
      control = true;
    }
  }
  if (temperature > 0 && control)
  {
    if ((temp - 0.5) >= temperature)
    {
      digitalWrite(RELAY, LOW);
    }
    else if ((temp - 0.5) <= (temperature - 1))
    {
      digitalWrite(RELAY, HIGH);
    }
  }
  delay(500);
}
