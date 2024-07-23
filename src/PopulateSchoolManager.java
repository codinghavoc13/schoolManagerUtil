import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Duration;
import java.time.DayOfWeek;
import java.io.BufferedReader;
import java.io.FileNotFoundException;

public class PopulateSchoolManager {
    public record AddressUserPair(Long address_id, Long user_id) {}
    // public record CourseInfo(Long courseId, String courseName, double credit, String courseBlock) {}
    public record CourseInfo(String courseName, double credit, String courseBlock) {}
    public record CoursePreReqPair(Long courseId, Long prereqId){}
    // public record CPTInfo(int cptId, int courseId, int period, int teacherId){}
    public record CPTInfo(int courseId, int period, int teacherId){}
    public record ParentStudentPair(Long parent_id, Long student_id, String relation) {}
    public record UserPair(Long user1, Long user2) {}

    /** Number of staff accounts to create */
    private static int NUM_ADM = 5;
    /** Number of super administrator accounts to create. */
    private static int NUM_SU = 1;
    /** Number of teacher accounts to create.*/
    private static int NUM_TEACHERS = 86;
    private static LocalDate START_OF_SCHOOL_YEAR = LocalDate.of(2024, 8, 19);
    /** Sum total of super and normal admin and teachers to create. */
    private static int TOTAL_NUM_STAFF = NUM_SU + NUM_ADM + NUM_TEACHERS;
    /**
     * Total number of records to generate. More SU, admin and teachers will mean
     * fewer students and parents.
     */
    private static int TOTAL_RECORDS_TO_GENERATE = 600;

    private static LocalDateTime start;
    private static LocalDateTime stop;

    private static Long addressId = 1l;
    // private static Long relationshipId = 1l;
    private static Long userId = 1l;

    private static int ctrAssignments = 0;
    private static int ctrParents = 0;
    private static int ctrStaff = 0;
    private static int ctrStudents = 0;
    private static int ctrTeachers = 0;
    private static int phoneNumberprefix = 100;
    private static int phoneNumberExchange = 0;
    private static int schoolIdCtr = 1;
    private static int userNameSuffix = 1;

    private static HashMap<Long, Object> addressList = new HashMap<>();
    
    private static ArrayList<AddressUserPair> addressUserList = new ArrayList<>();
    private static ArrayList<String> apartmentList = new ArrayList<>();
    private static ArrayList<String> apartmentStreetNames = new ArrayList<>(Arrays.asList("West Montana St",
        "East Washington Place", "West Oregon Ave", "East California Dr"));
    private static ArrayList<CourseInfo> courses = new ArrayList<>();
    private static ArrayList<CoursePreReqPair> coursePreReqList = new ArrayList<>();
    private static ArrayList<CPTInfo> cptList = new ArrayList<>();
    private static ArrayList<String> firstNames = new ArrayList<>();
    private static ArrayList<String> gradeLevels = new ArrayList<>(
        Arrays.asList("K", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12"));
    private static ArrayList<String> lastNames = new ArrayList<>();
    private static ArrayList<ParentStudentPair> parentStudentList = new ArrayList<>();
    private static ArrayList<String> streetList = new ArrayList<>();
    private static ArrayList<String> streetNames = new ArrayList<>(
        Arrays.asList("West Alabama St", "East Arizona Place", "West Arkansas Ave", "East Alaska Dr"));
    private static ArrayList<Long> teacherIdList = new ArrayList<>();
    private static ArrayList<String> userNamesTest = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        start();
    }    

    public static void start() {
        startClock();
        // boolean test = true;
        boolean test = false;
        if (test) {
            testRunner();
        } else {
            readNamesFromFile();
            buildUserInsertStatements();
            buildRelationshipInsertStatements();
            buildAddressInsertStatement();
            buildParentAddressInsertStatement();
            generateAssignments();
            generateCourses();
            //disabling until new course generation method is working
            // generateCoursePeriodTeacher();
        }
        stopClock();
    }

    private static void testRunner() {
        System.out.println("No methods for testing right now");
    }

    private static void buildAddressInsertStatement() {
        // INSERT INTO school_manager.address(address_1, address_2, city, state,
        // zip_code) VALUES (?, ?, ?, ?, ?);
        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO school_manager.address(address_1, address_2, city, state, zip_code) VALUES \n");
        String addressLine1 = "";
        String addressLine2 = "";
        for (Map.Entry<Long, Object> entry : addressList.entrySet()) {
            if (entry.getValue() instanceof String) {
                addressLine1 = "'" + entry.getValue().toString() + "', ";
                addressLine2 = "null, ";
            }
            if (entry.getValue() instanceof String[]) {
                addressLine1 = "'" + ((String[]) entry.getValue())[0] + "', ";
                addressLine2 = "'" + ((String[]) entry.getValue())[1] + "', ";
            }
            sb.append("(");
            sb.append(addressLine1);
            sb.append(addressLine2);
            sb.append("'Lakewood', ");
            sb.append("'CO', ");
            sb.append("'12345'), \n");
        }

        String result = sb.toString();
        result = result.substring(0, result.lastIndexOf(")") + 1);

        FileWriter writer;
        try {
            writer = new FileWriter("output/insertAddress.sql");
            writer.write(result);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String buildIndividualUser(String firstName, String lastName, String grade, String role,
            String studentSchoolId) {
        StringBuilder sb = new StringBuilder();
        String username = firstName.toLowerCase() + "." + lastName.toLowerCase();
        username = checkUserName(username);
        // this is for testing only, live version checks against the database
        userNamesTest.add(username);
        String[] pw = PasswordHashUtil.hashPWWPBKDF("password");

        sb.append("(");
        // sb.append(userId + ", ");// user_id
        sb.append("'" + firstName + "', ");// first_name
        sb.append("'" + lastName + "', ");// last_name
        sb.append(grade);// grade
        sb.append("'" + generatePhoneString() + "', ");// phone
        sb.append("'" + pw[1] + "', ");// pw_hash
        sb.append("'" + pw[0] + "', ");// pw_salt
        // sb.append("'pw_hash', ");//pw_hash
        // sb.append("'pw_salt', ");//pw_salt
        sb.append("'" + role + "', ");// role
        sb.append(studentSchoolId);// schoolid
        sb.append("'" + username + "', ");// username
        sb.append("true, ");// verified
        sb.append("'" + username + "@email.com'),\n");// email_string
        userId++;
        return sb.toString();
    }

    private static void buildParentAddressInsertStatement() {
        // INSERT INTO school_manager.address_user(address_id, user_id)VALUES (?, ?);
        StringBuilder sb = new StringBuilder();
        sb.append("INSERT into school_manager.address_user (address_id, user_id) VALUES \n");

        for (AddressUserPair au : addressUserList) {
            sb.append("(" + au.address_id + ", " + au.user_id + "),\n");
        }

        String result = sb.toString();
        result = result.substring(0, result.lastIndexOf(")") + 1);

        FileWriter writer;
        try {
            writer = new FileWriter("output/insertAddressUser.sql");
            writer.write(result);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void buildRelationshipInsertStatements() {
        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO school_manager.relationships(");
        // sb.append("relation_id, student_id, relative_id, relationship) ");
        sb.append("student_id, relative_id, relationship) ");
        sb.append("VALUES \n");
        for (ParentStudentPair ps : parentStudentList) {
            sb.append("(");
            // sb.append(relationshipId + ", ");
            // relationshipId++;
            sb.append(ps.student_id + ", ");// student_id
            sb.append(ps.parent_id + ", ");// relative_id
            sb.append("'" + ps.relation + "'),\n");
        }

        /*
         * CANCEL: other contact list (other parent, aunt/uncle, grandparent, other
         * emergency contacts)
         * moving non-user relations to a different table to be built later
         */

        /*
         * sibling list
         * - go through studentSiblingList, create pairs
         */

        String result = sb.toString();
        result = result.substring(0, result.lastIndexOf(")") + 1);

        FileWriter writer;
        try {
            writer = new FileWriter("output/insertRelationships.sql");
            writer.write(result);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void buildUserInsertStatements() {
        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO school_manager.users(");
        // sb.append("user_id, first_name, last_name, grade_level, phone, pw_hash, pw_salt, role, school_student_id, username, verified, email_string) ");
        sb.append("first_name, last_name, grade_level, phone, pw_hash, pw_salt, role, school_student_id, username, verified, email_string) ");
        sb.append("VALUES \n");
        String firstName = "";
        String lastName = "";
        String role = "";
        String studentSchoolId = "";
        String grade = "";
        Random rand = new Random();
        int rf = rand.nextInt(firstNames.size());
        int rl = rand.nextInt(lastNames.size());
        int ctr = 0;
        Long primaryId = 1l;
        Long parentId = 1l;
        int studentCtr = 0;
        while ((ctr < TOTAL_RECORDS_TO_GENERATE)) {
            rf = rand.nextInt(firstNames.size());
            firstName = firstNames.get(rf);
            lastName = lastNames.get(rl);
            if (ctr == 0) {
                role = "SUPER_ADMIN";
                studentSchoolId = "null, ";
                grade = "null, ";
                sb.append(buildIndividualUser(firstName, lastName, grade, role, studentSchoolId));
                ctr++;
                ctrStaff++;
                rl = rand.nextInt(lastNames.size());
            } else if (ctr > 0 && ctr < (NUM_SU + NUM_ADM)) {
                role = "ADMIN";
                studentSchoolId = "null, ";
                grade = "null, ";
                generateAddress();
                addressUserList.add(new AddressUserPair(addressId, userId));
                addressId++;
                sb.append(buildIndividualUser(firstName, lastName, grade, role, studentSchoolId));
                ctr++;
                ctrStaff++;
                rl = rand.nextInt(lastNames.size());
            } else if (ctr > NUM_ADM && ctr < TOTAL_NUM_STAFF) {
                role = "TEACHER";
                studentSchoolId = "null, ";
                grade = "null, ";
                generateAddress();
                addressUserList.add(new AddressUserPair(addressId, userId));
                addressId++;
                teacherIdList.add(userId);
                sb.append(buildIndividualUser(firstName, lastName, grade, role, studentSchoolId));
                ctr++;
                ctrTeachers++;
                rl = rand.nextInt(lastNames.size());
            } else {
                /*
                 * Look at modifying this section to include adding additional relations
                 * (emergency contacts, grandparents, aunt/uncle, etc)
                 * This will be addressed later, I'm going to add another entity and junction
                 * table to handle non-users (people that we want to keep track of but will not
                 * have login credentials)
                 */
                // primary
                role = "PRIMARY";
                studentSchoolId = "null, ";
                grade = "null, ";
                generateAddress();
                addressUserList.add(new AddressUserPair(addressId, userId));
                primaryId = (long) ctr + 1;
                sb.append(buildIndividualUser(firstName, lastName, grade, role, studentSchoolId));
                ctr++;
                ctrParents++;
                // parent
                role = "PARENT";
                parentId = (long) ctr + 1;
                rf = rand.nextInt(firstNames.size());
                firstName = firstNames.get(rf);
                addressUserList.add(new AddressUserPair(addressId, userId));
                sb.append(buildIndividualUser(firstName, lastName, grade, role, studentSchoolId));
                ctr++;
                ctrParents++;
                addressId++;
                /*
                 * Modify this section to create sibling relationships when studentCtr > 1
                 * - create a new arraylist named siblings
                 * - set to null of studentCtr == 1
                 * - set to new list if studentCtr > 1
                 * - come back to this later
                 */
                studentCtr = rand.nextInt(1, 4);
                while (studentCtr > 0) {
                    rf = rand.nextInt(firstNames.size());
                    firstName = firstNames.get(rf);
                    role = "STUDENT";
                    studentSchoolId = "'" + generateSchoolId() + "', ";
                    String gradeLevel = gradeLevels.get(rand.nextInt(gradeLevels.size()));
                    grade = "'" + gradeLevel + "', ";
                    parentStudentList.add(new ParentStudentPair(primaryId, (long) ctr + 1, "PRIMARY"));
                    parentStudentList.add(new ParentStudentPair(parentId, (long) ctr + 1, "PARENT"));
                    studentCtr--;
                    sb.append(buildIndividualUser(firstName, lastName, grade, role, studentSchoolId));
                    ctr++;
                    ctrStudents++;
                }
                rl = rand.nextInt(lastNames.size());
            }
        }

        String result = sb.toString();
        result = result.substring(0, result.lastIndexOf(")") + 1);
        FileWriter writer;
        try {
            writer = new FileWriter("output/insertUsers.sql");
            writer.write(result);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String checkUserName(String username) {
        if (!userNamesTest.contains(username)) {
            return username;
        } else {
            boolean valid = false;
            String tempName = "";
            while (!valid) {
                tempName = username + String.valueOf(userNameSuffix);
                if (!userNamesTest.contains(tempName)) {
                    username = tempName;
                    valid = true;
                    userNameSuffix = 1;
                } else {
                    userNameSuffix++;
                }
            }
        }
        return username;
    }

    private static void generateAddress() {
        Random rand = new Random();
        if (rand.nextDouble() > 0.5) {
            String address = generateStreetAddress();
            addressList.put(addressId, address);
        } else {
            String[] arr = generateTwoPartStreetAddress();
            addressList.put(addressId, arr);
        }
    }
    
    /*
     * TODO May need to rework this an all other methods that explicitly set the <object>_id since this
     * appears to be causing a issue where the auto increment value isn't being updated correctly. This
     * is working for dropping bulk data but, in this case, when I try to add an assignment via the portal,
     * the auto id is starting at 1; also need to throw in some reports
     */
    private static void generateAssignments() {
        LocalDate date = START_OF_SCHOOL_YEAR;
        StringBuilder sb = new StringBuilder();
        String type = "";
        String title = "";
        int hwCtr = 1;
        int quizCtr = 1;
        int testCtr = 1;
        int rptCtr = 1;
        boolean build = true;
        // INSERT INTO school_manager.assignment(assignment_id, teacher_id,
        // assignment_title, assignment_type, assignment_due_date) VALUES (?, ?, ?, ?,
        // ?);
        // sb.append("INSERT INTO school_manager.assignment"
        //         + "(assignment_id, teacher_id, assignment_title, assignment_type, assignment_due_date) VALUES \n");
        sb.append("INSERT INTO school_manager.assignment"
                + "(teacher_id, assignment_title, assignment_type, assignment_due_date) VALUES \n");
        for (Long teacherId : teacherIdList) {
            // reset the date at the beginning of each run or else the due date gets really
            // far out
            date = START_OF_SCHOOL_YEAR;
            for (int i = 0; i < 60; i++) {
                switch (date.getDayOfWeek()) {
                    case DayOfWeek.MONDAY:
                    case DayOfWeek.TUESDAY:
                    case DayOfWeek.THURSDAY:
                        type = "'HOMEWORK', ";
                        title = "'Homework" + " " + hwCtr + "', ";
                        hwCtr++;
                        break;
                    case DayOfWeek.WEDNESDAY:
                        type = "'QUIZ', ";
                        title = "'Quiz" + " " + quizCtr + "', ";
                        quizCtr++;
                        break;
                    case DayOfWeek.FRIDAY:
                        if(i%3==0){
                            type = "'REPORT', ";
                            title = "'Report " + rptCtr + "', ";
                            rptCtr++;
                        } else {
                            type = "'TEST', ";
                            title = "'Test" + " " + testCtr + "', ";
                            testCtr++;
                        }
                        break;
                    default:
                        build = false;
                        break;
                }
                if (build) {
                    sb.append("(");
                    sb.append(teacherId + ", ");
                    sb.append(title);
                    sb.append(type);
                    sb.append("'" + date.toString() + "'), \n");
                    ctrAssignments++;
                }
                build = true;
                date = date.plusDays(1);
            }
        }
        String result = sb.toString();
        result = result.substring(0, result.lastIndexOf(")") + 1);

        FileWriter writer;
        try {
            writer = new FileWriter("output/insertAssignments.sql");
            writer.write(result);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
     * TODO - I did not update this with the cpt table updates, need to make some massive changes
     * - Need to first create the entries for the courses: id, block, name, credit
     * - Teachers need to be generated and put in the list
     * - Then need to read from a new file the pairings of course id, teacher id, period
     */
    private static void generateCourses() {
        readCoursesFromFile();
        StringBuilder coursesSb = new StringBuilder();
        StringBuilder preReqSb = new StringBuilder();
        String[] electiveGenResults = new String[2];
        Long courseId = 1l;
        String courseName = "";
        String courseCredit = "";
        coursesSb.append("INSERT INTO school_manager.course(course_name, credit, course_block) VALUES \n");
        preReqSb.append("INSERT INTO school_manager.course_pre_req(course_id, prereq_id) VALUES \n");
        for (CourseInfo courseInfo : courses) {
            courseName = "'" + courseInfo.courseName + "',";
            courseCredit = String.valueOf(courseInfo.credit);
            coursesSb.append("(");
            coursesSb.append(courseId + ",");
            coursesSb.append(courseName);
            // sb.append(courseCredit + ", ");
            // sb.append("'" + courseInfo.courseBlock + "'),\n");
            coursesSb.append(courseCredit + ",'FULL_YEAR'),\n");
            if(courseId == 1){
                preReqSb.append("(" + courseId + ",-1),\n");
            } else if (courseId > 7 && courseId % 6 == 2){
                preReqSb.append("(" + courseId + ",7),\n");
            } else {
                preReqSb.append("(" + courseId + "," + (courseId - 1) + "),\n");
            }
            courseId++;
        }

        /*
         * TODO want to rework this method to 'hard code' the world languages with split semester courses (101 and 103
         * in the fall, 102 and 204 in the spring) or have the world languages only teach 4 classes per day and have
         * all four levels available each semester
         */
        /*
         * TODO Need to update enum on backend to include SEMESTER
         */
        String[] languages = new String[]{"German","French","Spanish"};
        String[] levels = new String[]{"101","102","103","104"};
        for (String language : languages){
            for(String level : levels){
                courseName = courseId + ",'" + language + " - " + level + "',";
                coursesSb.append("(");
                coursesSb.append(courseName);
                coursesSb.append("0.5,'SEMESTER'),\n");
                if(level.equals(levels[0])){
                    preReqSb.append("(" + courseId + ",7),\n");
                } else {
                    preReqSb.append("(" + courseId + "," + ( courseId - 1) + "),\n");
                }
                courseId++;
            }
        }
        
         //pass in the count of how many courses have been generated so far
        electiveGenResults = generateElectives(courseId);
        coursesSb.append(electiveGenResults[0]);
        preReqSb.append(electiveGenResults[1]);

        String coursesResult = coursesSb.toString();
        coursesResult = coursesResult.substring(0, coursesResult.lastIndexOf(")") + 1);
        String preReqResult = preReqSb.toString();
        preReqResult = preReqResult.substring(0, preReqResult.lastIndexOf(")") + 1);

        FileWriter writer;
        try {
            writer = new FileWriter("output/insertCourses.sql");
            writer.write(coursesResult);
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            writer = new FileWriter("output/buildCoursePreReq.sql");
            writer.write(preReqResult);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
     * TODO This needs massive rework
     */
    private static void generateCoursePeriodTeacher() {
        readCPTFromFile();
        StringBuilder sb = new StringBuilder();
        // INSERT INTO school_manager.course_period_teacher(ct_id, course_id, period, teacher_id) VALUES (?, ?, ?, ?);
        // sb.append("INSERT INTO school_manager.course_period_teacher(cpt_id, course_id, period, teacher_id) VALUES \n");
        sb.append("INSERT INTO school_manager.course_period_teacher(course_id, period, teacher_id) VALUES \n");
        for(CPTInfo cpt : cptList){
            // sb.append("(" + cpt.cptId + ", ");
            sb.append("(");
            sb.append(cpt.courseId + ", ");
            sb.append(cpt.period + ", ");
            sb.append(teacherIdList.get(cpt.teacherId) + "), \n");
        }
        String result = sb.toString();
        result = result.substring(0, result.lastIndexOf(")") + 1);

        FileWriter writer;
        try {
            writer = new FileWriter("output/buildCoursePeriodTeacher.sql");
            writer.write(result);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String[] generateElectives(Long electiveIdCounter){
        String[] result = new String[2];
        String ELECTIVE_PREFIX = "Elective";
        int electiveCounter = 1;
        StringBuilder coursesSB = new StringBuilder();
        StringBuilder prereqSB = new StringBuilder();
        for (int i = 0; i < 30; i++){
                    coursesSB.append("(" + electiveIdCounter +",'" + ELECTIVE_PREFIX + " " + electiveCounter + " 101',0.5,'SEMESTER'),\n");
                    prereqSB.append("(" + electiveIdCounter + ",7),\n");
                    electiveIdCounter++;coursesSB.append("(" + electiveIdCounter +",'" + ELECTIVE_PREFIX + " " + electiveCounter + " 102',0.5,'SEMESTER'),\n");
                    prereqSB.append("(" + electiveIdCounter + "," + (electiveIdCounter - 1) +"),\n");
                    electiveIdCounter++;
            electiveCounter++;
        }
        for (int i = 0; i < 15; i++){
                coursesSB.append("(" + electiveIdCounter +",'" + ELECTIVE_PREFIX + " " + electiveCounter + " 101',1,'FULL_YEAR'),\n");
                prereqSB.append("(" + electiveIdCounter + ",7),\n");
                electiveIdCounter++;
            electiveCounter++;
        }
        result[0] = coursesSB.toString();
        result[1] = prereqSB.toString();
        return result;
    }

    private static String generatePhoneString() {
        StringBuilder sb = new StringBuilder();
        sb.append("720-");
        sb.append(phoneNumberprefix + "-");
        sb.append(padString(String.valueOf(phoneNumberExchange), 4));
        phoneNumberExchange++;
        if (phoneNumberExchange == 10000) {
            phoneNumberExchange = 0;
            phoneNumberprefix++;
        }
        return sb.toString();
    }

    private static String generateSchoolId() {
        StringBuilder sb = new StringBuilder();
        sb.append("abc");
        sb.append(padString(String.valueOf(schoolIdCtr), 9));
        schoolIdCtr++;
        return sb.toString();
    }

    private static String generateStreetAddress() {
        Random rand = new Random();
        int r = rand.nextInt(streetNames.size());
        int houseNumber = 1;
        String tempAddr = String.valueOf(houseNumber) + " " + streetNames.get(r);
        if (!streetList.contains(tempAddr)) {
            streetList.add(tempAddr);
            return tempAddr;
        } else {
            boolean valid = false;
            while (!valid) {
                tempAddr = String.valueOf(houseNumber) + " " + streetNames.get(r);
                if (!streetList.contains(tempAddr)) {
                    streetList.add(tempAddr);
                    valid = true;
                } else {
                    houseNumber++;
                }
            }
        }
        return tempAddr;
    }

    private static String[] generateTwoPartStreetAddress() {
        String[] result = new String[2];
        Random rand = new Random();
        int r = rand.nextInt(apartmentStreetNames.size());
        int houseNumber = 1;
        int aptNum = 1;
        String street = apartmentStreetNames.get(r);
        String fullAddr = houseNumber + " " + street + " Apt: " + aptNum;
        if (!apartmentList.contains(fullAddr)) {
            apartmentList.add(fullAddr);
            result[0] = houseNumber + " " + street;
            result[1] = "Apt: " + aptNum;
        } else {
            boolean valid = false;
            while (!valid) {
                fullAddr = houseNumber + " " + street + "Apt: " + aptNum;
                if (!apartmentList.contains(fullAddr)) {
                    apartmentList.add(fullAddr);
                    valid = true;
                    result[0] = houseNumber + " " + street;
                    result[1] = "Apt: " + aptNum;
                } else {
                    aptNum++;
                    if (aptNum > 12) {
                        aptNum = 1;
                        houseNumber++;
                    }
                }
            }
        }
        return result;
    }

    public static String padString(String input, int padding) {
        if (input.length() >= padding) {
            return input;
        }
        StringBuilder sb = new StringBuilder();
        while (sb.length() < (padding - input.length())) {
            sb.append('0');
        }
        sb.append(input);
        return sb.toString();
    }

    private static void readCoursesFromFile(){
        String line = "";
        String courseName = "";
        double credit;
        String courseBlock = "";
        try {
            BufferedReader br = new BufferedReader(new FileReader("resources/courses.csv"));
            while((line = br.readLine()) != null){
                String[] arr = line.split(",");
                if(arr.length>1){
                    //ignore column 0, this is a id value for reference
                    courseName = arr[1];
                    credit = Double.valueOf(arr[2]);
                    courseBlock = arr[3];
                    courses.add(new CourseInfo(courseName, credit, courseBlock));
                }
            }
            br.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void readCPTFromFile(){
        /*
         * Need to make changes to the backend to include a column to track full year vs
         * fall or spring semester
         */
        String line = "";
        try {
            BufferedReader br = new BufferedReader(new FileReader("resources/cpt.csv"));
            while((line = br.readLine()) != null){
                if(line.indexOf(",") > -1){
                    String[] arr = line.split(",");
                    // public record CPTInfo(int courseId, int period, int teacherId){}
                    cptList.add(new CPTInfo(Integer.parseInt(arr[0]),
                    Integer.parseInt(arr[1]), Integer.parseInt(arr[2])));
                }
            }
            br.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void readNamesFromFile(){
        String line = "";
        try {
            BufferedReader br = new BufferedReader(new FileReader("resources/firstNamesMale.txt"));
            while((line = br.readLine()) != null){
                firstNames.add(line);
            }
            br.close();
            br = new BufferedReader(new FileReader("resources/firstNamesFemale.txt"));
            while((line = br.readLine()) != null){
                firstNames.add(line);
            }
            br.close();
            br = new BufferedReader(new FileReader("resources/lastNames.txt"));
            while((line = br.readLine()) != null){
                lastNames.add(line);
            }
            br.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void startClock(){
        start = LocalDateTime.now();
    }

    private static void stopClock(){
        stop = LocalDateTime.now();
        summary();
    }

    private static void summary(){
        StringBuilder sb = new StringBuilder();
        sb.append("------------------Script complete------------------\n");
        sb.append("----------------------Summary----------------------\n");
        sb.append("-> Staff created:         " + ctrStaff + "\n");
        sb.append("-> Teachers generated:    " + ctrTeachers + "\n");
        sb.append("-> Parents generated:     " + ctrParents + "\n");
        sb.append("-> Students generated:    " + ctrStudents + "\n");
        sb.append("-> Assignments generated: " + ctrAssignments + "\n");
        // sb.append(null);
        sb.append("-> Total run time:        " + calcRunTime() + " seconds\n");
        sb.append("--------------------End Summary--------------------\n");
        System.out.println(sb.toString());
    }

    private static String calcRunTime(){
        Duration duration = Duration.between(start, stop);
        return duration.getSeconds() + ":" + duration.getNano();
    }
}