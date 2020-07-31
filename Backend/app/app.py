from flask import Flask, jsonify, request
import pymysql
import threading
import queue
from socket import socket

sema_freetable = threading.Semaphore()

#Flask init
app = Flask(__name__)

#DB tables
usertable = "user_table"
friendtable = "friend_table"
freetable = "free_table"

#DB attributes
user_id = "id"
friend_id = "f_id"
free_day = "day"
start_time = "start"
end_time = "end"

#success message
success_msg = "Done"

#leader message
leader_msg = "Leader"

#error messages
error_msg = {"no_parm":("Error: No Parameters Provided",400),
             "no_id":("Error: No ID Provided",400),
             "no_fid":("Error: No Friend ID Provided", 400),
             "no_name":("Error: No Name Provided",400),
             "no_day":("Error: No Day Provided", 400),
             "no_start":("Error: No Start time Provided", 400),
             "no_end":("Error: No End time Provided", 400),
             "no_session":("Error: No session Provided", 400),
             "day_time_mis":("Error: Day and Time list mismatch", 400),
             "id_dup":("Error: Duplicate ID", 400),
             "id_none":("Error: Non existing ID", 400),
             "json_none":("Error: No JSON Provided", 400),
             "fid_none":("Error: Non existing Friend ID", 400),
             "rel_exist":("Error: Already Friend", 400),
             "date_wrong":("Error: Date out of bounds", 400),
             "time_wrong":("Error: Time out of bounds", 400),
             "session_none":("Error: Session not available", 400),
             "insert_fail":("Error: Insert Failed", 500),
             "db_down":("Error: DB Server Down", 500),
             "session_full":("Error: Too many sessions", 500)}

#Make a query to DB Server
#Returns query result
#Input may be a list of strings or a single string
def query(sql):

    #Result list
    result = []

    #Connection info
    connection = pymysql.connect(
        host='localhost',
        port=3306,
        user='testuser',
        password='testpw',
        db='testdb',
        charset='utf8',
        cursorclass=pymysql.cursors.DictCursor
    )

    #Do query
    try:
        with connection.cursor() as cursor:
            #Check if input is list
            if type(sql) is list:
                for q in sql:
                    cursor.execute(q)
                    currresult = cursor.fetchall()
                    result.append(currresult)
            else:
                cursor.execute(sql)
                result = cursor.fetchall()
    except:
        #If error occurred
        connection.rollback()
        connection.close()
        return None
    else:
        #If everything is done
        connection.commit()
        connection.close()
        return result

#Check server status, aka Ping-Pong
@app.route("/ping",methods=["GET"])
def pingpong():
    #Check if DB server is online
    dbcon = query("show tables")

    #If DB server is offline, return error
    if dbcon is None:
        return error_msg["db_down"]

    #If fine, return pong
    return "pong"

#Insert user info into DB
#Returns status message
def insert_user(id, name):
    #Check if user already exists
    find = query(f"select * from {usertable} where {user_id}={id}")
    if len(find)>0:
        return error_msg["id_dup"]

    #Insert user
    res = query(f"insert into {usertable} values({id}, '{name}')")

    #Check if insertion is a success
    if res is not None:
        return success_msg
    else:
        return error_msg["insert_fail"]

#Insert friend relation into DB
#Returns status message
def insert_relation(id, fid):
    #Check if user ID exists
    findid = query(f"select * from {usertable} where {user_id}={id}")
    if len(findid)==0:
        return error_msg["id_none"]

    #Check if friend ID exists
    findfid = query(f"select * from {usertable} where {user_id}={fid}")
    if len(findfid) == 0:
        return error_msg["fid_none"]

    #Check if id-fid pair already exists
    findf = query(f"select * from {friendtable} where {user_id}={id} and {friend_id}={fid}")
    if len(findf)>0:
        return error_msg["rel_exist"]

    #Insert relation
    res = query(f"insert into {friendtable} values({id}, {fid})")

    #Check if insertion is a success
    if res is not None:
        return success_msg
    else:
        return error_msg["insert_fail"]

#Converts int to day enum
def int2day(day):
    if day==0: return "MON"
    elif day==1: return "TUE"
    elif day==2: return "WED"
    elif day==3: return "THU"
    elif day==4: return "FRI"
    elif day==5: return "SAT"
    elif day==6: return "SUN"
    else: return None

#Checks if time is in bounds
def checktime(time):
    return (time>=0 and time<2400)

#Insert freetime into DB
#Returns status message
def insert_freetime(id, freetime):
    #Check if id exists
    find = query(f"select * from {usertable} where {user_id}={id}")
    if len(find)==0:
        return error_msg["id_none"]

    #list for query strings
    qlist = []

    #Check each freetime
    #If every item is legal, add a query statement to qlist
    #If not, return error
    for (day, start, end) in freetime:
        #Check if day is valid
        if int2day(day) is None:
            return error_msg["date_wrong"]

        #Check if time is legal
        if (checktime(start) and checktime(end)) == False:
            return error_msg["time_wrong"]

        #Everything is okay, now add query statement
        qlist.append(f"insert into {freetable} values({id}, '{int2day(day)}', {start}, {end})")

    #Do insert
    res = query(qlist)

    #Check if insertion failed
    if res is None:
        return error_msg["insert_fail"]

    #If success, return success message
    return success_msg

#Get intersection between to time lists
#Returns list of dictionary
def getinter(timelist, targlist):
    #list for results
    candidate = []

    for time in timelist:
        day = time[0]
        start = time[1]
        end = time[2]

        for targ in targlist:
            if targ[0] == day:
                targstart = targ[1]
                targend = targ[2]

                #Find start time
                if start>targstart: candstart = start
                else: candstart = targstart

                #Find end time
                if end<targend: candend = end
                else: candend = targend

                #Check if time slice is legal
                if candstart<candend:
                    if {"day":day, "start":candstart, "end":candend} not in candidate:
                        candidate.append({"day":day, "start":candstart, "end":candend})
    return candidate

#Add user using json request
@app.route("/test-j",methods=["POST"])
def test():
    #Json request
    now = request.json

    #Check if Json is in correct form
    if now is None:
        return error_msg["no_parm"]

    if "id" not in now:
        return error_msg["no_id"]

    if "name" not in now:
        return error_msg["no_name"]

    #Do insert
    id = now["id"]
    name = now["name"]
    return insert_user(id, name)

#Add user using query request
@app.route("/test-q",methods=["POST"])
def testq():
    #Get id and name
    id = request.args.get("id")
    name = request.args.get("name")

    #Check if id and name exist in arguments
    if id is None:
        return error_msg["no_id"]
    if name is None:
        return error_msg["no_name"]

    #Do insert
    return insert_user(id, name)

#Set friend relationship
@app.route("/test-setf", methods=["POST"])
def testsetf():
    #Get id and friend id
    id = request.args.get("id")
    fid = request.args.get("fid")

    #Check if id and fid exist in arguments
    if id is None:
        return error_msg["no_id"]
    if fid is None:
        return error_msg["no_fid"]

    #Do insert
    return insert_relation(id, fid)

#Set free time
@app.route("/test-sett", methods=["POST"])
def testsett():
    #Json for free time list
    json = request.json

    #Check is Json is in correct form
    if json is None:
        return error_msg["json_none"]
    if "id" not in json:
        return error_msg["no_id"]
    if "day" not in json:
        return error_msg["no_day"]
    if "start" not in json:
        return error_msg["no_start"]
    if "end" not in json:
        return error_msg["no_end"]

    #Get id
    id = json["id"]

    #Get list for day, start, end
    day = json["day"]
    start = json["start"]
    end = json["end"]

    #Make sure list length are all same
    if len(day)!=len(start) or len(day)!=len(end):
        return error_msg["day_time_mis"]

    #Make into list of tuple
    freetime = list(zip(day, start, end))

    #Lock when modifying freetable
    sema_freetable.acquire()
    query(f"delete from {freetable} where id={id}")
    res = insert_freetime(id, freetime)
    sema_freetable.release()

    #Return result
    return res

#Get matching freetime with friends
@app.route("/test-getft",methods=["GET"])
def testgetf():
    #Get id
    id = request.args.get("id")
    if id is None:
        return error_msg["no_id"]

    #Check if user exists
    find = query(f"select * from {usertable} where {user_id}={id}")
    if len(find)==0:
        return error_msg["id_none"]

    #Lock when accessing freetable
    sema_freetable.acquire()
    mytime = query(f"select * from {freetable} where {user_id}={id}")
    sema_freetable.release()

    #list for available freetimes
    result = []

    #If my time is not available, return empty Json
    if len(mytime)==0:
        return jsonify({"result":result})

    #List for mytime
    mytimelist = []

    #Convert mytime into mytimelist
    for my in mytime:
        mytimelist.append((my[free_day], my[start_time], my[end_time]))

    #Get friend list
    friendlist = query(f"select {friend_id} from {friendtable} where {user_id}={id}")

    #For each friend, get their free time
    for fid in friendlist:
        #Lock when accessing freetable
        sema_freetable.acquire()
        ftime = query(f"select * from {freetable} where {user_id}={fid[friend_id]}")
        sema_freetable.release()

        #List for friend's freetime
        ftimelist = []

        #Convert freetime into freetimelist
        for f in ftime:
            ftimelist.append((f[free_day], f[start_time], f[end_time]))

        #Get intersection of mytimelist and friend's freetimelist
        inter = getinter(mytimelist, ftimelist)

        #Append to result
        if len(inter)>0: result.append({"fid":fid[friend_id], "times":inter})

    #Return result
    return jsonify({"result":result})

#Max number of sessions
maxsession = 2

#Sessions list. Each session is a list of (id, ip)
sessions = []

#Available sessions number queue.
available = queue.Queue()

#Semaphores for accessing sessions
sema_sessions = []

#Semaphore for accessing available queue.
sema_available = threading.Semaphore()

#Initialize sessions and semaphores
for i in range(0,maxsession):
    available.put(i)
    sessions.append([])
    sema_sessions.append(threading.Semaphore())

tcpPort = 7000

#Make meeting session
@app.route("/test-make",methods=["GET"])
def testmake():
    #Get id
    id = request.args.get("id")
    if id is None:
        return error_msg["no_id"]

    #Check if an available session exists
    if available.empty():
        return error_msg["session_full"]

    #Lock when modifying availble list
    sema_available.acquire()
    session = available.get()
    sema_available.release()

    #Lock when modifying session
    sema_sessions[session].acquire()
    sessions[session].clear()
    #Add user info
    sessions[session].append((id, request.remote_addr))
    sema_sessions[session].release()

    #Return session number
    return jsonify({"session":session})

#Join session
@app.route("/test-join",methods=["POST"])
def testjoin():
    #Get id
    id = request.args.get("id")
    if id is None:
        return error_msg["no_id"]

    #Get session
    session = request.args.get("session")
    if session is None:
        return error_msg["no_session"]

    #Convert session into int
    session = eval(session)

    #Lock when modifying session
    sema_sessions[session].acquire()
    #Check if session is valid
    if len(sessions[session]) == 0:
        sema_sessions[session].release()
        return error_msg["session_none"]

    # Add user info
    if (id, request.remote_addr) not in sessions[session]:
        sessions[session].append((id, request.remote_addr))
    printall()
    for s in (sessions[session]):
        ip = s[1]
        data = str(len(sessions[session])).encode()
        print(data)
        threading.Thread(connect(ip, data)).start()

    sema_sessions[session].release()

    # Check if user is leader
    if (id, request.remote_addr) == sessions[session][0]:
        return leader_msg

    #If not, return done
    return success_msg

#Connect to user ip
def connect(ip, data):
    soc = socket()
    print("connecting: "+ip)
    soc.connect((ip, tcpPort))
    print("connected: "+ip)
    soc.sendall(data)
    soc.close()

#End session
@app.route("/test-end",methods=["POST"])
def testend():
    #Get session
    session = request.args.get("session")
    if session is None:
        return error_msg["no_session"]

    #Convert session into int
    session = eval(session)

    #Lock when modifying session
    sema_sessions[session].acquire()
    #Check if session is valid
    if len(sessions[session]) == 0:
        sema_sessions[session].release()
        return error_msg["session_none"]

    #List for freetimes
    freetimes = []

    for id, ip in sessions[session]:
        #Lock when accessing freetable
        sema_freetable.acquire()
        freelist = query(f"select * from {freetable} where {user_id}={id}")
        sema_freetable.release()

        #Current user's freetime list
        freetime = []

        #Convert into freetimelist
        for ft in freelist:
            freetime.append((ft[free_day], ft[start_time], ft[end_time]))

        #Append to result
        freetimes.append(freetime)

    #Get all intersections
    temp = freetimes[0]
    if len(freetimes) == 1:
        return jsonify(temp)
    for ft in freetimes[1:]:
        inter = getinter(temp, ft)
        temp = convertinter(inter)

    # Send result via socket
    #for id, ip in sessions[session]:
     #   data = str(temp).encode()
     #   print(data)
      #  threading.Thread(connect(ip, data, True)).start()

    ip = sessions[session][0][1]
    data = str(temp).encode()
    print(data)
    threading.Thread(connect(ip, data)).start()

    # Clear session
    sessions[session].clear()
    sema_sessions[session].release()

    # Return session number
    sema_available.acquire()
    available.put(session)
    sema_available.release()

    #Return result
    return jsonify(temp)

#Convert getinter result into list of tuples
def convertinter(inter):
    result = []
    for i in inter:
        result.append((i["day"], i["start"], i["end"]))
    return result

#For debug
def printall():
    for i, s in enumerate(sessions):
        if len(s)>0:
            print(i)
            print(s)
