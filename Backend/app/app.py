from typing import Optional, Callable, Any, Iterable, Mapping

from flask import Flask, jsonify, request
import pymysql
import threading
import queue
from socket import socket

#Flask init
app = Flask(__name__)

#DB tables
usertable = "user_table"
friendtable = "friend_table"
freetable = "free_table"
rmtable = "remove_table"

#DB attributes
user_id = "id"
friend_id = "f_id"
free_day = "day"
start_time = "start"
end_time = "end"
user_name = "name"

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
             "no_item":("Error: No item Provided", 400),
             "no_time":("Error: No time Provided", 400),
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
#Deletes current info before inserting
#Returns status message
def insert_freetime(id, freetime):
    #Check if id exists
    find = query(f"select * from {usertable} where {user_id}={id}")
    if len(find)==0:
        return error_msg["id_none"]

    #list for query strings
    qlist = []

    #Delete current rows
    qlist.append(f"delete from {freetable} where id={id}")

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

    #Start transaction
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

    #Insert info into freetime table
    res = insert_freetime(id, freetime)

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

    # Get user free time
    mytime = query(f"select * from {freetable} where {user_id}={id}")

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
    friendlist = query(f"select {friend_id},{user_name} from {friendtable} inner join {usertable} on {friend_id}={usertable}.{user_id} where {friendtable}.{user_id}={id}")

    #For each friend, get their free time
    for fid in friendlist:
        #Get friend's free time
        ftime = query(f"select * from {freetable} where {user_id}={fid[friend_id]}")

        #List for friend's freetime
        ftimelist = []

        #Convert freetime into freetimelist
        for f in ftime:
            ftimelist.append((f[free_day], f[start_time], f[end_time]))

        #Get intersection of mytimelist and friend's freetimelist
        inter = getinter(mytimelist, ftimelist)
        #Append to result
        if len(inter)>0:
            #Get removed item list
            rmlist = query(f"select {free_day}, {start_time}, {end_time} from {rmtable} where {user_id}={id} and {friend_id}={fid[friend_id]}")

            #Append to result only if not in rmlist
            interresult = []
            for i in inter:
                if i not in rmlist:
                    interresult.append(i)

            result.append({"fid":fid[friend_id], "name":fid[user_name],"times":interresult})

    #Return result
    return jsonify({"result":result})

#Max number of sessions
maxsession = 2

#Sessions list. Each session is a list of (id, ip)
sessions = []

#Vote list. Each session contains list of int
votes = []

#Voted user count. Each session contains int
#-1 = user can enter
#0 = user cannot enter, voting started
votecounts = []

#Available sessions number queue.
available = queue.Queue()

#Semaphores for accessing sessions
sema_sessions = []

#Available ports for socket comm.
ports = queue.Queue()

#Ports assigned for session
givenports = []

#Number of ports given to session
portscount = []

#Semaphore for accessing ports count
sema_portcount = []

#Phases of threads
threadphases = []

#Results for sessions
sessionres = []

#Temp freetime for sessions.
#Each session contains a dict.
temptimes = []

#Base TCP Port
tcpPort = 7000

#max TCP Port
maxTcpPort = 10000

#Initialize sessions and semaphores
for i in range(0,maxsession):
    available.put(i)
    sessions.append([])
    sema_sessions.append(threading.Semaphore())
    votes.append([])
    votecounts.append(-1)
    givenports.append([])
    sessionres.append(b"")
    portscount.append(0)
    sema_portcount.append(threading.Semaphore())
    temptimes.append({})

#Initialize ports
for i in range(tcpPort, maxTcpPort):
    ports.put(i)
    threadphases.append(0)


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

    session = available.get()

    #Lock when modifying session
    sema_sessions[session].acquire()
    sessions[session].clear()
    votes[session].clear()
    votecounts[session] = -1
    temptimes[session].clear()
    #Add user info
    sessions[session].append((id, request.remote_addr ))
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

    #Check if session is closed
    if votecounts[session]>-1:
        sema_sessions[session].release()
        return error_msg["session_none"]

    # Add user info
    port = 0
    if ((id, request.remote_addr) not in sessions[session]) or ((id, request.remote_addr) == sessions[session][0]):
        if ((id, request.remote_addr) not in sessions[session]):
            sessions[session].append((id, request.remote_addr))
        # Get port
        port = ports.get()
        givenports[session].append(port)
        threading.Thread(target=ready_socket, args=(port, session)).start()

        sema_portcount[session].acquire()
        portscount[session] = portscount[session]+1
        sema_portcount[session].release()

    printall()

    sema_sessions[session].release()

    # Check if user is leader
    if (id, request.remote_addr) == sessions[session][0]:
        print(-port)
        return str(-port)

    #If not, return done
    print(port)
    return str(port)

#Connect to user ip
#deprecated
def connect_old(ip, data):
    try:
        soc = socket()
        if(ip == '172.30.1.50'):ip='127.0.0.1'
        print("connecting: "+ip)
        soc.connect((ip, tcpPort))
        print("connected: "+ip)
        soc.sendall(data)
        soc.close()
    except:
        print("connect failed: "+ip)

#Open socket for user
#Pass data as lambda
def ready_socket(port, session):
    with socket() as s:
        s.bind(("0.0.0.0", port))
        print(f"start listening port={port}")
        s.listen(1)
        conn, addr = s.accept()
        print(f"accepted: addr={addr}")
        predata = b""
        keepon = True
        while keepon:
            nowdata = b""
            # Entering phase
            if threadphases[port-tcpPort] == 0:
                nowdata = (str(len(sessions[session]))+"\n").encode()
            # Time result phase
            elif threadphases[port-tcpPort] == 1:
                nowdata = sessionres[session]
                threadphases[port - tcpPort] = 2
            # Vote count phase
            elif threadphases[port-tcpPort] == 2:
                nowdata = (str(votecounts[session])+"\n").encode()
            # Vote result phase
            elif threadphases[port-tcpPort] == 3:
                nowdata = str(votes[session]).encode()
                keepon = False
            #Send data
            if predata != nowdata:
                print(f"sending {nowdata}")
                conn.sendall(nowdata)
                predata = nowdata


            if s.fileno()<0:
                keepon = False

        #Reset phase
        threadphases[port-tcpPort] = 0

        sema_portcount[session].acquire()
        portscount[session] = portscount[session]-1
        sema_portcount[session].release()

        #Return port
        print(f"returning port={port}")
        ports.put(port)


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
        #Get freetime from temptimes
        freetime = temptimes[session][id]

        #Append to result
        freetimes.append(freetime)

    #Get all intersections
    temp = freetimes[0]

    #If only one member has joined, reset session.
    if len(freetimes) == 1:
        clearSession(session)
        return jsonify(temp)

    for ft in freetimes[1:]:
        inter = getinter(temp, ft)
        temp = convertinter(inter)

    # Set up votes
    for i in range(0, len(temp)):
        votes[session].append(0)

    # Change data and phase
    sessionres[session] = str(temp).encode()
    for i in givenports[session]:
        threadphases[i-tcpPort] = 1

    # Open for voting
    votecounts[session] = 0
    sema_sessions[session].release()

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

#Check if user exists
@app.route("/test-check", methods=["GET"])
def testcheck():
    # Get id
    id = request.args.get("id")
    if id is None:
        return error_msg["no_id"]

    #Search for user
    res = query(f"select * from {usertable} where {user_id}={id}")
    if res is None:
        return error_msg["db_down"]
    if len(res) == 0:
        return error_msg["id_none"]
    else:
        return success_msg

#Vote for session
@app.route("/test-vote", methods=["POST"])
def testvote():
    #Get session
    session = request.args.get("session")
    if session is None:
        return error_msg["no_session"]

    #Convert session into int
    session = eval(session)

    #Get voted item
    item = request.args.get("item")
    if item is None:
        return error_msg["no_item"]

    #Convert item into int
    item = eval(item)

    #Increase vote counts
    sema_sessions[session].acquire()
    votes[session][item] = votes[session][item]+1
    votecounts[session] = votecounts[session]+1
    sema_sessions[session].release()

    #If everyone voted, clear session
    if(len(sessions[session]) == votecounts[session]):

        # Wrap up session info
        clearSession(session)

    return success_msg

def returnapp():
    return app

def clearSession(session):
    for i in givenports[session]:
        threadphases[i - tcpPort] = 3

    # Wait until all ports are closed
    while portscount[session] > 0:
        True

    #Clear session info
    votes[session].clear()
    votecounts[session] = -1
    sessions[session].clear()
    givenports[session].clear()
    sessionres[session] = b""

    #Return session number
    available.put(session)

#Set temp free time
@app.route("/test-settt", methods=["POST"])
def testsettt():
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

    # Get session
    session = request.args.get("session")
    if session is None:
        return error_msg["no_session"]

    # Convert session into int
    session = eval(session)

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
    res = []
    for time in freetime:
        res.append((int2day(time[0]),time[1],time[2]))

    #Lock when modifying temptimes
    sema_sessions[session].acquire()
    temptimes[session][str(id)] = res
    print(temptimes[session])
    sema_sessions[session].release()

    #Return result
    return success_msg

#Insert removed recommendation into DB
#Returns status message
def insert_removed(id, fid, day, start, end):
    #Check if id exists
    find = query(f"select * from {usertable} where {user_id}={id}")
    if len(find)==0:
        return error_msg["id_none"]

    #Do insert
    res = query(f"insert into {rmtable} values({id},{fid},'{int2day(day)}', {start}, {end})")

    #Check if insertion failed
    if res is None:
        return error_msg["insert_fail"]

    #If success, return success message
    return success_msg

#Remove recommendation
@app.route("/test-rmrec", methods=["POST"])
def testrmrec():
    # Get id
    id = request.args.get("id")
    if id is None:
        return error_msg["no_id"]

    # Get fid
    fid = request.args.get("fid")
    if fid is None:
        return error_msg["no_fid"]

    # Get time
    time = request.args.get("time")
    if time is None:
        return error_msg["no_time"]
    time = eval(time)

    # deconstruct time
    day = time//100000000
    start = (time%100000000)//10000
    end = time%10000

    # Insert
    return insert_removed(id, fid, day, start, end)