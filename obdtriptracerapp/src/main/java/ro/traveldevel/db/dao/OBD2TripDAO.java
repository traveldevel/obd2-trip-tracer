package ro.traveldevel.db.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

import ro.traveldevel.db.entity.OBD2Trip;

@Dao
public interface OBD2TripDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<OBD2Trip> trips);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOne(OBD2Trip trip);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    public void updateTrips(OBD2Trip... trips);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    public void updateTrip(OBD2Trip trip);

    @Delete
    public void deleteTrips(OBD2Trip... trips);

    @Query("delete from OBD2Trip where id=:id")
    int deleteTrip(long id);

    @Query("SELECT * FROM OBD2Trip WHERE id = :tripId")
    OBD2Trip getTripById(long tripId);

    @Query("SELECT * FROM OBD2Trip ORDER BY id DESC")
    List<OBD2Trip> getAllTrips();

    @Query("SELECT * FROM OBD2Trip WHERE sent = 0 ORDER BY id ASC")
    List<OBD2Trip> getTripsToBeSent();
}