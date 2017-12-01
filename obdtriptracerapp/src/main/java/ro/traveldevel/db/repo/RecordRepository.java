package ro.traveldevel.db.repo;

import android.arch.persistence.room.Room;
import android.content.Context;

import java.util.List;

import ro.traveldevel.config.Constants;
import ro.traveldevel.db.AppDatabase;
import ro.traveldevel.db.entity.OBD2Record;

public class RecordRepository {

    Context context;
    AppDatabase appDatabase;

    public RecordRepository(Context context) {
        appDatabase = Room.databaseBuilder(context, AppDatabase.class, Constants.DATABASE_NAME).build();
        context = context;
    }

    public  void refreshRepository(){
        if(context != null){
            appDatabase = Room.databaseBuilder(context, AppDatabase.class, Constants.DATABASE_NAME).build();
        }
    }

    public void createRecord(OBD2Record record){
        appDatabase.recordDao().insertOne(record);
    }

    public void updateRecord(OBD2Record record){
        appDatabase.recordDao().updateRecord(record);
    }

    public void updateRecords(OBD2Record... records){
        appDatabase.recordDao().updateRecords(records);
    }

    public void deleteRecords(OBD2Record... records){
        appDatabase.recordDao().deleteRecords(records);
    }

    public int deleteRecord(long id){
        return appDatabase.recordDao().deleteRecord(id);
    }

    public List<OBD2Record> getAllRecords(){
        List<OBD2Record> records = appDatabase.recordDao().getAllRecords();
        return records;
    }

    public List<OBD2Record> getRecordsToBeSent(){
        List<OBD2Record> records = appDatabase.recordDao().getRecordsToBeSent();
        return records;
    }

    public OBD2Record getRecordById(long id){
        OBD2Record record = appDatabase.recordDao().getRecordById(id);
        return record;
    }
}