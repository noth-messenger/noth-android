package org.thoughtcrime.securesms.database.helpers;


import android.content.Context;
import android.database.Cursor;

import androidx.annotation.NonNull;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteDatabaseHook;
import net.sqlcipher.database.SQLiteOpenHelper;

import org.thoughtcrime.securesms.crypto.DatabaseSecret;
import org.thoughtcrime.securesms.database.AttachmentDatabase;
import org.thoughtcrime.securesms.database.DraftDatabase;
import org.thoughtcrime.securesms.database.GroupDatabase;
import org.thoughtcrime.securesms.database.GroupReceiptDatabase;
import org.thoughtcrime.securesms.database.JobDatabase;
import org.thoughtcrime.securesms.database.MmsDatabase;
import org.thoughtcrime.securesms.database.OneTimePreKeyDatabase;
import org.thoughtcrime.securesms.database.PushDatabase;
import org.thoughtcrime.securesms.database.RecipientDatabase;
import org.thoughtcrime.securesms.database.SearchDatabase;
import org.thoughtcrime.securesms.database.SessionDatabase;
import org.thoughtcrime.securesms.database.SignedPreKeyDatabase;
import org.thoughtcrime.securesms.database.SmsDatabase;
import org.thoughtcrime.securesms.database.StickerDatabase;
import org.thoughtcrime.securesms.database.ThreadDatabase;
import org.session.libsignal.utilities.logging.Log;
import org.thoughtcrime.securesms.loki.database.LokiAPIDatabase;
import org.thoughtcrime.securesms.loki.database.LokiBackupFilesDatabase;
import org.thoughtcrime.securesms.loki.database.LokiMessageDatabase;
import org.thoughtcrime.securesms.loki.database.LokiThreadDatabase;
import org.thoughtcrime.securesms.loki.database.LokiUserDatabase;
import org.thoughtcrime.securesms.loki.database.SharedSenderKeysDatabase;
import org.thoughtcrime.securesms.loki.protocol.ClosedGroupsMigration;

public class SQLCipherOpenHelper extends SQLiteOpenHelper {

  @SuppressWarnings("unused")
  private static final String TAG = SQLCipherOpenHelper.class.getSimpleName();

  // First public release (1.0.0) DB version was 27.
  // So we have to keep the migrations onwards.
  private static final int lokiV7                           = 28;
  private static final int lokiV8                           = 29;
  private static final int lokiV9                           = 30;
  private static final int lokiV10                          = 31;
  private static final int lokiV11                          = 32;
  private static final int lokiV12                          = 33;
  private static final int lokiV13                          = 34;
  private static final int lokiV14_BACKUP_FILES             = 35;
  private static final int lokiV15                          = 36;
  private static final int lokiV16                          = 37;
  private static final int lokiV17                          = 38;
  private static final int lokiV18_CLEAR_BG_POLL_JOBS       = 39;
  private static final int lokiV19                          = 40;
  private static final int lokiV20                          = 41;

  // Loki - onUpgrade(...) must be updated to use Loki version numbers if Signal makes any database changes
  private static final int    DATABASE_VERSION = lokiV20;
  private static final String DATABASE_NAME    = "signal.db";

  private final Context        context;
  private final DatabaseSecret databaseSecret;

  public SQLCipherOpenHelper(@NonNull Context context, @NonNull DatabaseSecret databaseSecret) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION, new SQLiteDatabaseHook() {
      @Override
      public void preKey(SQLiteDatabase db) {
        db.rawExecSQL("PRAGMA cipher_default_kdf_iter = 1;");
        db.rawExecSQL("PRAGMA cipher_default_page_size = 4096;");
      }

      @Override
      public void postKey(SQLiteDatabase db) {
        db.rawExecSQL("PRAGMA kdf_iter = '1';");
        db.rawExecSQL("PRAGMA cipher_page_size = 4096;");
      }
    });

    this.context        = context.getApplicationContext();
    this.databaseSecret = databaseSecret;
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL(SmsDatabase.CREATE_TABLE);
    db.execSQL(MmsDatabase.CREATE_TABLE);
    db.execSQL(AttachmentDatabase.CREATE_TABLE);
    db.execSQL(ThreadDatabase.CREATE_TABLE);
    db.execSQL(DraftDatabase.CREATE_TABLE);
    db.execSQL(PushDatabase.CREATE_TABLE);
    db.execSQL(GroupDatabase.CREATE_TABLE);
    db.execSQL(RecipientDatabase.CREATE_TABLE);
    db.execSQL(GroupReceiptDatabase.CREATE_TABLE);
    db.execSQL(OneTimePreKeyDatabase.CREATE_TABLE);
    db.execSQL(SignedPreKeyDatabase.CREATE_TABLE);
    db.execSQL(SessionDatabase.CREATE_TABLE);
    for (String sql : SearchDatabase.CREATE_TABLE) {
      db.execSQL(sql);
    }
    for (String sql : JobDatabase.CREATE_TABLE) {
      db.execSQL(sql);
    }
    db.execSQL(StickerDatabase.CREATE_TABLE);

    db.execSQL(LokiAPIDatabase.getCreateSnodePoolTableCommand());
    db.execSQL(LokiAPIDatabase.getCreateOnionRequestPathTableCommand());
    db.execSQL(LokiAPIDatabase.getCreateSwarmTableCommand());
    db.execSQL(LokiAPIDatabase.getCreateLastMessageHashValueTable2Command());
    db.execSQL(LokiAPIDatabase.getCreateReceivedMessageHashValuesTable3Command());
    db.execSQL(LokiAPIDatabase.getCreateOpenGroupAuthTokenTableCommand());
    db.execSQL(LokiAPIDatabase.getCreateLastMessageServerIDTableCommand());
    db.execSQL(LokiAPIDatabase.getCreateLastDeletionServerIDTableCommand());
    db.execSQL(LokiAPIDatabase.getCreateDeviceLinkCacheCommand());
    db.execSQL(LokiAPIDatabase.getCreateUserCountTableCommand());
    db.execSQL(LokiAPIDatabase.getCreateSessionRequestTimestampCacheCommand());
    db.execSQL(LokiAPIDatabase.getCreateSessionRequestSentTimestampTableCommand());
    db.execSQL(LokiAPIDatabase.getCreateSessionRequestProcessedTimestampTableCommand());
    db.execSQL(LokiAPIDatabase.getCreateOpenGroupPublicKeyTableCommand());
    db.execSQL(LokiAPIDatabase.getCreateOpenGroupProfilePictureTableCommand());
    db.execSQL(LokiAPIDatabase.getCreateClosedGroupEncryptionKeyPairsTable());
    db.execSQL(LokiAPIDatabase.getCreateClosedGroupPublicKeysTable());
    db.execSQL(LokiMessageDatabase.getCreateMessageIDTableCommand());
    db.execSQL(LokiMessageDatabase.getCreateMessageToThreadMappingTableCommand());
    db.execSQL(LokiMessageDatabase.getCreateErrorMessageTableCommand());
    db.execSQL(LokiThreadDatabase.getCreateSessionResetTableCommand());
    db.execSQL(LokiThreadDatabase.getCreatePublicChatTableCommand());
    db.execSQL(LokiUserDatabase.getCreateDisplayNameTableCommand());
    db.execSQL(LokiUserDatabase.getCreateServerDisplayNameTableCommand());
    db.execSQL(LokiBackupFilesDatabase.getCreateTableCommand());
    db.execSQL(SharedSenderKeysDatabase.getCreateOldClosedGroupRatchetTableCommand());
    db.execSQL(SharedSenderKeysDatabase.getCreateCurrentClosedGroupRatchetTableCommand());
    db.execSQL(SharedSenderKeysDatabase.getCreateClosedGroupPrivateKeyTableCommand());

    executeStatements(db, SmsDatabase.CREATE_INDEXS);
    executeStatements(db, MmsDatabase.CREATE_INDEXS);
    executeStatements(db, AttachmentDatabase.CREATE_INDEXS);
    executeStatements(db, ThreadDatabase.CREATE_INDEXS);
    executeStatements(db, DraftDatabase.CREATE_INDEXS);
    executeStatements(db, GroupDatabase.CREATE_INDEXS);
    executeStatements(db, GroupReceiptDatabase.CREATE_INDEXES);
    executeStatements(db, StickerDatabase.CREATE_INDEXES);
  }

  @Override
  public void onConfigure(SQLiteDatabase db) {
    super.onConfigure(db);
    // Loki - Enable write ahead logging mode and increase the cache size.
    // This should be disabled if we ever run into serious race condition bugs.
    db.enableWriteAheadLogging();
    db.execSQL("PRAGMA cache_size = 10000");
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    Log.i(TAG, "Upgrading database: " + oldVersion + ", " + newVersion);

    db.beginTransaction();

    try {

      if (oldVersion < lokiV7) {
        db.execSQL(LokiMessageDatabase.getCreateErrorMessageTableCommand());
      }

      if (oldVersion < lokiV8) {
        db.execSQL(LokiAPIDatabase.getCreateSessionRequestTimestampCacheCommand());
      }

      if (oldVersion < lokiV9) {
        db.execSQL(LokiAPIDatabase.getCreateSnodePoolTableCommand());
        db.execSQL(LokiAPIDatabase.getCreateOnionRequestPathTableCommand());
      }

      if (oldVersion < lokiV10) {
        db.execSQL(LokiAPIDatabase.getCreateSessionRequestSentTimestampTableCommand());
        db.execSQL(LokiAPIDatabase.getCreateSessionRequestProcessedTimestampTableCommand());
      }

      if (oldVersion < lokiV11) {
        db.execSQL(LokiAPIDatabase.getCreateOpenGroupPublicKeyTableCommand());
      }

      if (oldVersion < lokiV12) {
        db.execSQL(LokiAPIDatabase.getCreateLastMessageHashValueTable2Command());
        db.execSQL(SharedSenderKeysDatabase.getCreateCurrentClosedGroupRatchetTableCommand());
        db.execSQL(SharedSenderKeysDatabase.getCreateClosedGroupPrivateKeyTableCommand());
      }

      if (oldVersion < lokiV13) {
        db.execSQL(LokiAPIDatabase.getCreateReceivedMessageHashValuesTable3Command());
      }

      if (oldVersion < lokiV14_BACKUP_FILES) {
        db.execSQL(LokiBackupFilesDatabase.getCreateTableCommand());
      }

      if (oldVersion < lokiV15) {
        db.execSQL(SharedSenderKeysDatabase.getCreateOldClosedGroupRatchetTableCommand());
      }
      
      if (oldVersion < lokiV16) {
        db.execSQL(LokiAPIDatabase.getCreateOpenGroupProfilePictureTableCommand());
      }

      if (oldVersion < lokiV17) {
        db.execSQL("ALTER TABLE part ADD COLUMN audio_visual_samples BLOB");
        db.execSQL("ALTER TABLE part ADD COLUMN audio_duration INTEGER");
      }

      if (oldVersion < lokiV18_CLEAR_BG_POLL_JOBS) {
        // BackgroundPollJob was replaced with BackgroundPollWorker. Clear all the scheduled job records.
        db.execSQL("DELETE FROM job_spec WHERE factory_key = 'BackgroundPollJob'");
        db.execSQL("DELETE FROM constraint_spec WHERE factory_key = 'BackgroundPollJob'");
      }

      // Many classes were removed. We need to update DB structure and data to match the code changes.
      if (oldVersion < lokiV19) {
        db.execSQL(LokiAPIDatabase.getCreateClosedGroupEncryptionKeyPairsTable());
        db.execSQL(LokiAPIDatabase.getCreateClosedGroupPublicKeysTable());
        ClosedGroupsMigration.INSTANCE.perform(db);
        db.execSQL("DROP TABLE identities");
        deleteJobRecords(db, "RetrieveProfileJob");
        deleteJobRecords(db,
                "RefreshAttributesJob",
                "RotateProfileKeyJob",
                "RefreshUnidentifiedDeliveryAbilityJob",
                "RotateCertificateJob"
        );
      }

      if (oldVersion < lokiV20) {
        deleteJobRecords(db,
                "CleanPreKeysJob",
                "RefreshPreKeysJob",
                "CreateSignedPreKeyJob",
                "RotateSignedPreKeyJob",
                "MultiDeviceBlockedUpdateJob",
                "MultiDeviceConfigurationUpdateJob",
                "MultiDeviceContactUpdateJob",
                "MultiDeviceGroupUpdateJob",
                "MultiDeviceOpenGroupUpdateJob",
                "MultiDeviceProfileKeyUpdateJob",
                "MultiDeviceReadUpdateJob",
                "MultiDeviceStickerPackOperationJob",
                "MultiDeviceStickerPackSyncJob",
                "MultiDeviceVerifiedUpdateJob",
                "ServiceOutageDetectionJob",
                "SessionRequestMessageSendJob"
        );
      }

      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }
  }

  public SQLiteDatabase getReadableDatabase() {
    return getReadableDatabase(databaseSecret.asString());
  }

  public SQLiteDatabase getWritableDatabase() {
    return getWritableDatabase(databaseSecret.asString());
  }

  public void markCurrent(SQLiteDatabase db) {
    db.setVersion(DATABASE_VERSION);
  }

  private void executeStatements(SQLiteDatabase db, String[] statements) {
    for (String statement : statements)
      db.execSQL(statement);
  }

  private static boolean columnExists(@NonNull SQLiteDatabase db, @NonNull String table, @NonNull String column) {
    try (Cursor cursor = db.rawQuery("PRAGMA table_info(" + table + ")", null)) {
      int nameColumnIndex = cursor.getColumnIndexOrThrow("name");

      while (cursor.moveToNext()) {
        String name = cursor.getString(nameColumnIndex);

        if (name.equals(column)) {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * Cleans up all the records related to the job keys specified.
   * This method should be called once the Signal job class is deleted from the project.
   */
  private static void deleteJobRecords(SQLiteDatabase db, String... jobKeys) {
    for (String jobKey : jobKeys) {
      db.execSQL("DELETE FROM job_spec WHERE factory_key = ?", new String[]{jobKey});
      db.execSQL("DELETE FROM constraint_spec WHERE factory_key = ?", new String[]{jobKey});
    }
  }
}
