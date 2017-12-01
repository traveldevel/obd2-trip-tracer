package ro.traveldevel.db.entity;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

@Entity
public class OBD2Record {

    @PrimaryKey(autoGenerate = true)
    public long id;
    public int sent;
    public String obdVin;
    public long UTCTicks;
    public int manualOdometer;

    public int obdSpeed;
    public int obdRpm;
    public int obdThrotlePosition;
    public int obdEngineLoad;
    public int obdCoolantTemp;
    public int obdOilTemp;

    public double gpsLatitude;
    public double gpsLongitude;
    public int gpsAltitude;
    public int gpsSpeed;
    public int gpsBearing;
    public int gpsAccuracy;
    public String orientDir;

    public float accelerationX;
    public float accelerationY;
    public float accelerationZ;
    public double accelerationTotal;

    public OBD2Record(){
        this.sent = 0;
    }
}
