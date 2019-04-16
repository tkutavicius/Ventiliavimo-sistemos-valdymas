#define ABSZERO 273.15
#define MAXANALOGREAD 1023.0
#define ANALOGPIN A1

float temperature_NTC(float T0, float R0, float T1, float R1, float RV, float VA_VB)
{
  T0 += ABSZERO;
  T1 += ABSZERO;
  float B = (T0 * T1) / (T1 - T0) * log(R0 / R1);
  float RN = RV * VA_VB / (1 - VA_VB);
  return T0 * B / (B + T0 * log(RN / R0)) - ABSZERO;
}

void setup()
{
  Serial.begin(9600);   
}

void loop()
{
  float T0 = 25.0;
  float R0 = 10000.0;
  float T1 = 100.0;
  float R1 = 1000.0;
  float Vorwiderstand = 10000.0;

  float temp;
  int aValue = analogRead(ANALOGPIN);
  
  temp = temperature_NTC(T0, R0, T1, R1, Vorwiderstand, aValue / MAXANALOGREAD);
  Serial.println(temp);
  delay(1000);
}
