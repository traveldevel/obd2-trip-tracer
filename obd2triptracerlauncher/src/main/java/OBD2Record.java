import android.arch.persistence.room.Entity;

import java.sql.Timestamp;

@Entity
public class OBD2Record {

    @PrimaryKey(autoGenerate = true)
    public long id;
    public Timestamp timestamp;

    public int obdSpeed;
    public int obdRpm;
    public int obdThrotle;
    public int obdEngineLoad;

    public int latitude;
    public int longitude;
    public int altitude;
    public int gpsSpeed;
    public int gpsHeading;
    public int gpsAccuracy;

    public float accelerationX;
    public float accelerationY;
    public float accelerationZ;
}
