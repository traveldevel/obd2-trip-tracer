package ro.traveldevel.db;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.TypeConverters;

import ro.traveldevel.db.dao.OBD2RecordDAO;
import ro.traveldevel.db.dao.OBD2TripDAO;
import ro.traveldevel.db.entity.OBD2Record;
import ro.traveldevel.db.entity.OBD2Trip;

@Database(version = 1, entities = {OBD2Record.class, OBD2Trip.class})
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase{
    abstract public OBD2RecordDAO recordDao();
    abstract public OBD2TripDAO tripDao();
}
