package ro.traveldevel.db.entity;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

@Entity
public class OBD2Trip {

    @PrimaryKey(autoGenerate = true)
    public long id;
    public int sent;
    public String obdVin;
    public long startUTCTicks;
    public long endUTCTicks;
    public double totalMinutes;
    public int manualStartOdometer;
    public int estimatedEndOdometer;
    public double estimatedDistance;
    public double averageSpeed;

    public OBD2Trip(){
        this.sent = 0;
    }
}
