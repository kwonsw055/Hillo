from flask import Flask, jsonify, request
import pymysql

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

#error messages
error_msg = {"no_parm":("Error: No Parameters Provided",400),
             "no_id":("Error: No ID Provided",400),
             "no_fid":("Error: No Friend ID Provided", 400),
             "no_name":("Error: No Name Provided",400),
             "no_day":("Error: No Day Provided", 400),
             "no_start":("Error: No Start time Provided", 400),
             "no_end":("Error: No End time Provided", 400),
             "day_time_mis":("Error: Day and Time list mismatch", 400),
             "id_dup":("Error: Duplicate ID", 400),
             "id_none":("Error: Non existing ID", 400),
             "json_none":("Error: No JSON Provided", 400),
             "fid_none":("Error: Non existing Friend ID", 400),
             "rel_exist":("Error: Already Friend", 400),
             "date_wrong":("Error: Date out of bounds", 400),
             "time_wrong":("Error: Time out of bounds", 400),
             "insert_fail":("Error: Insert Failed", 500),
             "db_down":("Error: DB Server Down", 500)}

#Make a query to DB Server
#Returns query result
def query(sql):
    result = []
    connection = pymysql.connect(
        host='localhost',
        port=3306,
        user='testuser',
        password='testpw',
        db='testdb',
        charset='utf8',
        cursorclass=pymysql.cursors.DictCursor
    )
    try:
        with connection.cursor() as cursor:
            if type(sql) is list:
                print("is list")
                for q in sql:
                    cursor.execute(q)
                    currresult = cursor.fetchall()
                    result.append(currresult)
            else:
                cursor.execute(sql)
                result = cursor.fetchall()
    except:
        connection.rollback()
        connection.close()
        return None
    else:
        connection.commit()
        connection.close()
        return result

#Check server status, aka Ping-Pong
@app.route("/ping",methods=["GET"])
def pingpong():
    dbcon = query("show tables")
    if dbcon is None:
        return error_msg["db_down"]
    return "pong"

#Insert user info into DB
#Returns status message
def insert_user(id, name):
    find = query(f"select * from {usertable} where {user_id}={id}")
    if len(find)>0:
        return error_msg["id_dup"]
    res = query(f"insert into {usertable} values({id}, '{name}')")
    if res is not None:
        return success_msg
    else:
        return error_msg["insert_fail"]

#Insert friend relation into DB
#Returns status message
def insert_relation(id, fid):
    findid = query(f"select * from {usertable} where {user_id}={id}")
    if len(findid)==0:
        return error_msg["id_none"]
    findfid = query(f"select * from {usertable} where {user_id}={fid}")
    if len(findfid) == 0:
        return error_msg["fid_none"]
    findf = query(f"select * from {friendtable} where {user_id}={id} and {friend_id}={fid}")
    if len(findf)>0:
        return error_msg["rel_exist"]
    res = query(f"insert into {friendtable} values({id}, {fid})")
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

#Checks time is in bounds
def checktime(time):
    return (time>=0 and time<2400)

#Insert freetime into DB
#Returns status message
def insert_freetime(id, freetime):
    find = query(f"select * from {usertable} where {user_id}={id}")
    if len(find)==0:
        return error_msg["id_none"]
    qlist = []
    for (day, start, end) in freetime:
        if int2day(day) is None:
            return error_msg["date_wrong"]
        if (checktime(start) and checktime(end)) == False:
            return error_msg["time_wrong"]
        qlist.append(f"insert into {freetable} values({id}, '{int2day(day)}', {start}, {end})")
    print(qlist)
    res = query(qlist)
    if res is None:
        return error_msg["insert_fail"]
    return success_msg

#Get intersection between to time lists
def getinter(timelist, targlist):
    candidate = []
    for time in timelist:
        day = time[0]
        start = time[1]
        end = time[2]
        for targ in targlist:
            if targ[0] == day:
                targstart = targ[1]
                targend = targ[2]
                if start>targstart: candstart = start
                else: candstart = targstart
                if end<targend: candend = end
                else: candend = targend
                if candstart<candend:
                    if {"day":day, "start":candstart, "end":candend} not in candidate:
                        candidate.append({"day":day, "start":candstart, "end":candend})
    return candidate

#Add user using json request
@app.route("/test-j",methods=["POST"])
def test():
    now = request.json
    if now is None:
        return error_msg["no_parm"]
    if "id" not in now:
        return error_msg["no_id"]
    if "name" not in now:
        return error_msg["no_name"]
    id = now["id"]
    name = now["name"]
    return insert_user(id, name)

#Add user using query request
@app.route("/test-q",methods=["POST"])
def testq():
    id = request.args.get("id")
    name = request.args.get("name")
    if id is None:
        return error_msg["no_id"]
    if name is None:
        return error_msg["no_name"]
    return insert_user(id, name)

#Set friend relationship
@app.route("/test-setf", methods=["POST"])
def testsetf():
    id = request.args.get("id")
    fid = request.args.get("fid")
    if id is None:
        return error_msg["no_id"]
    if fid is None:
        return error_msg["no_fid"]
    return insert_relation(id, fid)

#Set free time
@app.route("/test-sett", methods=["POST"])
def testsett():
    json = request.json
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
    id = json["id"]
    day = json["day"]
    start = json["start"]
    end = json["end"]
    if len(day)!=len(start) or len(day)!=len(end):
        return error_msg["day_time_mis"]
    freetime = list(zip(day, start, end))
    print(freetime)
    return insert_freetime(id, freetime)

#Get matching freetime with friends
@app.route("/test-getft",methods=["GET"])
def testgetf():
    id = request.args.get("id")
    if id is None:
        return error_msg["no_id"]
    find = query(f"select * from {usertable} where {user_id}={id}")
    if len(find)==0:
        return error_msg["id_none"]
    mytime = query(f"select * from {freetable} where {user_id}={id}")
    print(mytime)
    result = []
    if len(mytime)==0:
        return jsonify({"result":result})
    mytimelist = []
    for my in mytime:
        mytimelist.append((my[free_day], my[start_time], my[end_time]))
    friendlist = query(f"select {friend_id} from {friendtable} where {user_id}={id}")
    for fid in friendlist:
        ftime = query(f"select * from {freetable} where {user_id}={fid[friend_id]}")
        ftimelist = []
        for f in ftime:
            ftimelist.append((f[free_day], f[start_time], f[end_time]))
        inter = getinter(mytimelist, ftimelist)
        if len(inter)>0: result.append({"fid":fid[friend_id], "times":inter})
    return jsonify({"result":result})