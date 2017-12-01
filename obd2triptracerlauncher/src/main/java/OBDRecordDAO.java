
@Dao
public interface OBDRecordDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<OBDRecord> record);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOne(OBDRecord record);
}
