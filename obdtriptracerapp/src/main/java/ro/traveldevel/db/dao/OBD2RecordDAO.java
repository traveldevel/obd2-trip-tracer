package ro.traveldevel.db.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

import ro.traveldevel.db.entity.OBD2Record;

@Dao
public interface OBD2RecordDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<OBD2Record> record);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOne(OBD2Record record);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    public void updateRecords(OBD2Record... records);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    public void updateRecord(OBD2Record record);

    @Delete
    public void deleteRecords(OBD2Record... records);

    @Query("delete from OBD2Record where id=:id")
    int deleteRecord(long id);

    @Query("SELECT * FROM OBD2Record WHERE id = :recordId")
    OBD2Record getRecordById(long recordId);

    @Query("SELECT * FROM OBD2Record ORDER BY id DESC")
    List<OBD2Record> getAllRecords();

    @Query("SELECT * FROM OBD2Record WHERE sent = 0 ORDER BY id ASC")
    List<OBD2Record> getRecordsToBeSent();
}