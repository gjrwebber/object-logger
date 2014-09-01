#Object Logger

## What is is?

The Object Logger is a logging framework for logging Objects to a data source. It also contains some nice functions to read from the log file. 

##How to use it?

The `ObjectLogger` is the entry point and requires an `IDataSource` to write the data to. One `ObjectLogger` will log all Objects in the same way to one file. Objects are logged with a timestamp, so in actual fact the object that is logged is a `TimestampedObject` which essentially just wraps the logged Object and contains a timestamp of when it was created.

There is one implementation of `IDataSource` available for use out of the box and that is the `FileSystemDataSource` which, you guessed it, writes to the file system. This data source takes a filename and `ITimestampedObjectSerialiser` to serialise the Object to the file. The default filename is the simple name of the generic type of the `ObjectLogger`. The default `ISerialiser` is the `TimestampedObjectJsonSerialiser`. 

The `FileSystemDataSource` can also take an `IRollingStrategy` to make decisions on when to roll the log file. By default the `DailyRollingStrategy` is used, but you can choose to use a `MinuteRollingStrategy` if you prefer. 

The `IDataSource` also contains methods to read the Objects from the log file. The `getAll()` method and its various overloaded counterparts allow the caller to read in all or some of the Objects from the file. THe Objects are returned in a `TimestampedObjectSet` which again provides the caller with more ways to retrieve the data they are looking for.

###Logging Example

Say we have an `Account` object that we want to log as JSON to the filesystem. This is how we would do it.


    public class Account {
        private String name;
        private long id;
        // --- emitted for clarity
    }

    public class AccountLogger {
        public static void main(String[] args) {
            
            // Name of the file starts with "accounts". Default 
            FileSystemDataSource<Account> dataSource = new FileSystemDataSource<Account>("accounts");		
            
            // This will log files in ~/logs/[yyyy-mm-dd]/...
            dataSource.setFileSystemLoggerPath("~/logs");
            
            // This will log Account objects to a file named ~/logs/[yyyy-mm-dd]/accounts-HH-MM.json as a JSON string.
            ObjectLogger<Account> logger = new ObjectLogger<Account>(dataSource);
            
            Account account = new Account("Jane", 12345);
            logger.log(account);
            Account account = new Account("Gary", 43251);
            logger.log(account);
            Account account = new Account("Gary", 9876);
            logger.log(account);
        }
    }

If you open the log file it will look something like:

    [
    {"logTime":1382942847149,"obj":{"name":"Jane","id":12345}},
    {"logTime":1382942847150,"obj":{"name":"Gary","id":43251}},
    {"logTime":1382942847151,"obj":{"name":"Gary","id":9876}}
    ]

If you want to view the json you can use something like <http://jsonlint.com> to format it before opening it in your favourite text reader.


### Reading Example

    public class AccountReader {
        public static void main(String[] args) {
            FileSystemDataSource<Account> dataSource = new FileSystemDataSource<Account>("accounts");
            
            // This will read files from ~/logs/[yyyy-mm-dd]/...
            dataSource.setFileSystemLoggerPath("~/logs");		
            TimestampedObjectSet<Account> accounts = dataSource.getAll(type);
            
            Set<TimestampedObject<Account>> uniqueAccounts = accounts.getUniqueForDate(new Date(), new Comparator<TimestampedObject<Account>>{
            
                @Override
                public int compare(TimestampedObject<Account> o1, TimestampedObject<Account> o2) {
                    
                    // Compare the names of the Accounts within the TimestampedObject
                    if (o1.getObj().getName().equals(o2.getObj().getName())) {
                        return 0;
                    }
                    return 1;
                }
            });
            
            for(TimestampedObject<Account> to : uniqueAccounts) {
                Account account = to.getObj();
                System.out.println(account.getName()+ " : " + account.getId())
            }
        }
    }

Should print:

	Jane : 12345
	Gary : 9876


