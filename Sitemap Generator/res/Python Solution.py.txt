
#load necessary python modules and check for import errors
importErrors = []


try:
    import mysql.connector
except ImportError:
    importErrors.append("[ERROR] You need to install python module : mysql.connector")
try:
    import time
except ImportError:
    importErrors.append("[ERROR] You need to install python module : time")


if len(importErrors) > 0:

    for err in importErrors:

        print err

    exit(1)

start = time.time()
filePath = "sitemap.txt"


def tryQuery(cursor, sqlQuery):

    try:

        cursor.execute(sqlQuery)
    except Exception, err:

        print repr(err)
        exit(1)


mysqlConfig = {
    "user": "----",
    "password": "----",
    "host": "----",  # data
    "port": 3306,
    "database": "data"  # data
}

# set up the mysql connection
try:

    print "    -Connecting to MySQL (idldev)."
    conn = mysql.connector.connect(**mysqlConfig)
except Exception, err:

    print repr(err)
    print "[ERROR] There an issue connecting to the MySQL (idldev) DB."
    exit(1)


cursor = conn.cursor()
stmt = "SELECT COUNT(*) FROM mand WHERE published = 1"
# run the mysql query
tryQuery(cursor, stmt)
line_count = cursor.fetchone()[0]
print(line_count)

stmt = "SELECT record_key FROM mand WHERE published = 1 ORDER BY id ASC"
tryQuery(cursor, stmt)
#rows = cursor.fetchall()
file = open(filePath,'w')
count = 0
for row in cursor:
    file.write(row[0])
    file.write("\n")
    count+=1
    if (count%1000==0):
        print(("{}: " + str(count) + " records written").format(time.time()-start))

file.close()
print("{}: File written, checking line count".format(time.time()-start))


with open(filePath) as f:

    for i, l in enumerate(f):

        pass

num_lines = i + 1

if (num_lines == line_count):
    print("Equal row count")
else:
    print("ERROR: Unequal row count")
    print(line_count + " lines expected")
    print(num_lines + " lines written")


print ("{}: program finished, file stored at: {}".format(time.time()-start, filePath))
