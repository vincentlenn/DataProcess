import com.csvreader.CsvWriter;

import java.io.*;
import java.util.*;


public class dataProcess {
    /**
     * 遍历对应文件夹下txt文件
     * 读取txt内容
     *
     * @param files 文件名称
     * @return 返回文件内容
     */
    public static LinkedHashMap<String, String> getFileContent(File files) {
        StringBuilder temp = new StringBuilder();
        LinkedHashMap<String, String> m1 = new LinkedHashMap<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(files));//构造一个BufferedReader类来读取文件
            String s = null;
            while ((s = br.readLine()) != null) {//使用readLine方法，一次读一行
                if (!s.isEmpty()) {
                    temp.append(s).append(System.lineSeparator());
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String result = String.valueOf(temp);
        int timeConsumeIndicatorsNum = result.indexOf("[耗时指标]");
        int performanceIndicatorsNum = result.indexOf("[性能指标]");
        int CPUFrequencyNum = result.indexOf("[CPU主要运行频率]");
        //        场景配置
        String sceneConf = result.substring(0, timeConsumeIndicatorsNum);
        String[] sceneConfiguration = sceneConf.split(System.lineSeparator());
        String scene = sceneConfiguration[0].split(":")[1];
        m1.put("sceneConfiguration", scene);


        //        耗时指标
        String timeConsum = result.substring(timeConsumeIndicatorsNum + 8, performanceIndicatorsNum);
        String[] timeConsumingIndicators = timeConsum.split(System.lineSeparator());
        for (int a = 0; a < timeConsumingIndicators.length; a++) {
            String[] tci = timeConsumingIndicators[a].replaceAll("\\n", "").split(":");
            if (tci[0].equals("Warning"))
            {
                continue;
            }
            m1.put(tci[0] + "_min", tci[3].split(",")[0]);
            m1.put(tci[0] + "_max", tci[4].split(",")[0]);
            m1.put(tci[0] + "_count", tci[5].split(",")[0]);
            m1.put(tci[0] + "_average", tci[6].split(",")[0]);
        }


        //         性能指标
        String perIndicators = result.substring(performanceIndicatorsNum + 7, CPUFrequencyNum);
        String[] performanceIndicators = perIndicators.split(System.lineSeparator());
        for (int b = 0; b < performanceIndicators.length; b++) {
            String[] pi = performanceIndicators[b].replaceAll("\\n", "").split(":");
            m1.put(pi[0] + "_Max", pi[2].split(",")[0]);
            m1.put(pi[0] + "_Min", pi[3].split(",")[0]);
            m1.put(pi[0] + "_Average", pi[4].split(",")[0]);
        }

        //        CPU运行频率
        String cpuFre = result.substring(CPUFrequencyNum + 12);
        String[] cpuFrequency = cpuFre.split(System.lineSeparator());
        m1.put("cpu0", cpuFrequency[0].split(":")[1]);
        m1.put("cpu4", cpuFrequency[4].split(":")[1]);
        return m1;
    }

    /**
     * 创建csv文件，写入内容
     *
     * @param
     * @return 返回文件内容
     */
    public static void writerCsvContent(File files, String csvTempFile,String[] headers) throws IOException {
        LinkedHashMap<String, String> linkedHashMap = getFileContent(files);
        File ff = new File(csvTempFile);

        //        新建csv文件,写入表头
        BufferedWriter writer = new BufferedWriter(new FileWriter(ff, true));
        CsvWriter cwriter = new CsvWriter(writer, ',');


        cwriter.writeRecord(headers, false);
        //        写入内容
        List<String> list = new ArrayList<>();
        for (int h = 0; h < headers.length; h++) {
            Iterator it = linkedHashMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();

                if (headers[h].equals(entry.getKey().toString())) {
                    String value = (String) entry.getValue();
                    list.add(value);
                    break;
                }
            }

        }
        cwriter.writeRecord(list.toArray(new String[list.size()]), false);
        cwriter.flush();
        cwriter.close();
    }
    /**
     * 处理csv文件为原始数据
     *
     * @param csvTempFile
     */
    public static void csvResult(String csvTempFile,String filePath) throws IOException {
        // 需要处理数据的文件位置
        FileReader fileReader = new FileReader(new File(csvTempFile));
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        Map<String, String> map = new LinkedHashMap<>();
        String readLine = null;
        int i = 0;
        while (true) {
            try {
                if (!((readLine = bufferedReader.readLine()) != null)) break;
            } catch (IOException e) {
                e.printStackTrace();
            }
            // 每次读取一行数据，与 map 进行比较，如果该行数据 map 中没有，就保存到 map 集合中
            if (!map.containsValue(readLine)) {
                map.put("key" + i, readLine);
                i++;
            }
        }
        File ff =new File(filePath+"result\\sourceData.csv");
        ff.delete();
        ff.createNewFile();
        BufferedWriter writer = new BufferedWriter(new FileWriter(ff, true));
        CsvWriter cwriter = new CsvWriter(writer, ',');
        for (int j = 0; j < map.size(); j++) {
            String[] AA = map.get("key" + j).split(",");
            cwriter.writeRecord(AA, false);
        }
        cwriter.flush();
        cwriter.close();
    }


    /**
     * 创建平均值csv文件，写入内容
     *
     * @param
     * @return 返回文件内容
     */
    public static void calculateAverage(String filePath,String[] headers) throws IOException {
        List<LinkedHashMap<String, String>> list1 = new LinkedList<>();
        if (true) {
            File file = new File(filePath);
            File[] fs = file.listFiles();
            for (File f : fs) {
                if (f.getName().endsWith(".txt")) {
                    LinkedHashMap<String, String> linkedHashMap = dataProcess.getFileContent(f);
                    list1.add(linkedHashMap);
                }
            }
            // 场景 --> 指标名称 --> 所有的值
            Map<String, Map<String, LinkedList<Double>>> result = new LinkedHashMap<>();
            LinkedList<Double> zhibiaoList = null;

            for (Map<String, String> map : list1) {
                for (Map.Entry<String, String> entry : map.entrySet()) {
                    String temp = entry.getKey();

                    // 过滤掉不需要的指标
                    if (!Arrays.asList(headers).contains(temp)) {
                        continue;
                    }
                    double zhiBiaoValue = 0;

                    try {
                        zhiBiaoValue = Double.parseDouble(entry.getValue());

                    } catch (Throwable e) {
                    }
                    Map<String, LinkedList<Double>> sceneMap = result.computeIfAbsent(map.get("sceneConfiguration"), k -> new LinkedHashMap<>());
                    zhibiaoList = sceneMap.computeIfAbsent(entry.getKey(), k -> new LinkedList<>());
                    zhibiaoList.add(zhiBiaoValue);
                }
            }

            // 场景 --> 指标名称 --> 平均值
            Map<String, Map<String, Double>> averageResult = new LinkedHashMap<>();
            for (Map.Entry<String, Map<String, LinkedList<Double>>> entry1 : result.entrySet()) {
                for (Map.Entry<String, LinkedList<Double>> entry2 : entry1.getValue().entrySet()) {
                    LinkedList<Double> val = entry2.getValue();
                    Map<String, Double> sceneMap = averageResult.computeIfAbsent(entry1.getKey(), k -> new LinkedHashMap<>());
                    sceneMap.put(entry2.getKey(), val.parallelStream().mapToDouble(Double::doubleValue).average().getAsDouble());
                }
            }
            File ff =new File(filePath+"result\\avg.csv");
            ff.delete();
            ff.createNewFile();

            //        新建csv文件,写入表头
            BufferedWriter writer = new BufferedWriter(new FileWriter(filePath+"result\\avg.csv", true));
            CsvWriter cwriter = new CsvWriter(writer, ',');


            cwriter.writeRecord(headers, false);

            // 遍历所有的场景，找到均值
            averageResult.forEach((key, value) -> {

                // 对于所有的header， 找到对应的均值
                for (String header : headers) {
                    String tempAvg;
                    if (header.equals("sceneConfiguration")) {
                        tempAvg = key;
                    } else {
                        tempAvg = value.get(header).toString();
                    }
                    try {
                        cwriter.write(tempAvg);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                // 不同场景，换行
                try {
                    cwriter.writeRecord(new String[]{""});
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            cwriter.close();
        }

    }


    public static void main(String[] args) throws IOException {
        /**
         * 主函数
         */
        String[] headers = {"sceneConfiguration", "cpuSystem_Max", "cpuSystem_Average", "cpu_com_iflytek_acp_Max", "cpu_com_iflytek_acp_Average",
                "pss_com_iflytek_acp_Max", "pss_com_iflytek_acp_Average", "wait_average", "raw1_average", "raw2_average", "cb_inv_average",
                "cb_im_average", "cb_sp_average", "generate cost_average",
                "generate interval_average", "matrix cost_average", "matrix interval_average",
                "Rate of audio data receiver_average", "engine audio data lost rate_average",
                "Heat map original data lost rate_average", "cpu0", "cpu4"};
        String filePath = "E:\\桌面\\性能测试工具\\data\\";

        //   每次执行前csv清空
        String csvTempFile = "E:\\桌面\\性能测试工具\\data\\result\\temp.csv";
        File csv = new File(csvTempFile);
        csv.delete();
        csv.createNewFile();

        // 处理txt文件
        File file = new File(filePath);
        File[] fs = file.listFiles();
        for (File f:fs){
            if(f.getName().endsWith(".txt")){
                writerCsvContent(f, csvTempFile,headers);
            }
        }
        csvResult(csvTempFile,filePath);
        calculateAverage(filePath,headers);
        System.out.println("执行完成");
    }
}
