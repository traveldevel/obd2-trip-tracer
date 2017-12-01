package ro.traveldevel.db.repo;

import android.arch.persistence.room.Room;
import android.content.Context;

import java.util.List;

import ro.traveldevel.config.Constants;
import ro.traveldevel.db.AppDatabase;
import ro.traveldevel.db.entity.OBD2Trip;

public class TripRepository {

    Context context;
    AppDatabase appDatabase;

    public TripRepository(Context context) {
        appDatabase = Room.databaseBuilder(context, AppDatabase.class, Constants.DATABASE_NAME).build();
        context = context;
    }

    public  void refreshRepository(){
        if(context != null){
            appDatabase = Room.databaseBuilder(context, AppDatabase.class, Constants.DATABASE_NAME).build();
        }
    }

    public void createTrip(OBD2Trip trip){
        appDatabase.tripDao().insertOne(trip);
    }

    public void updateTrip(OBD2Trip trip){
        appDatabase.tripDao().updateTrip(trip);
    }

    public void updateTrips(OBD2Trip... trips){
        appDatabase.tripDao().updateTrips(trips);
    }

    public void deleteTrips(OBD2Trip... trips){
        appDatabase.tripDao().deleteTrips(trips);
    }

    public int deleteTrips(long id){
        return appDatabase.tripDao().deleteTrip(id);
    }

    public List<OBD2Trip> getAllTrips(){
        List<OBD2Trip> trips = appDatabase.tripDao().getAllTrips();
        return trips;
    }

    public List<OBD2Trip> getTripsToBeSent(){
        List<OBD2Trip> trips = appDatabase.tripDao().getTripsToBeSent();
        return trips;
    }

    public OBD2Trip getTripById(long id){
        OBD2Trip trip = appDatabase.tripDao().getTripById(id);
        return trip;
    }
}