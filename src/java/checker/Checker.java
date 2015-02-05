
/*
 THIS DOES EVERYTHINGS ON THE SERVER, GET A PDF FILE, CONVERT AND CALCULATE
 */
/*
 * Copyright (C) 2015 Nguyen Minh Tien - minh-tien.nguyen@imag.fr
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package checker;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jws.WebService;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.xml.ws.WebServiceException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;
import javax.servlet.RequestDispatcher;
import javax.annotation.Resource;
import javax.servlet.ServletContext;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;




/**
 *
 * @author tien
 */
@WebService(serviceName = "Checker")
public class Checker {
@Resource
private WebServiceContext context;

    private String normalize(String name, String content) {
        
        content = content.toUpperCase();
        content = content.replaceAll("-", " ");// parenthesis like when they
        content = content.replaceAll("[^A-Z ]", "");
        // make a new line
        content = content.replaceAll("\n", " ");
        content = content.replaceAll("\\s+", " ");// remove extra spaces
        wordcount(name, content);
        return content;
    }

    private void wordcount(String name, String content) {

        String[] words = content.split(" ");
        HashMap<String, Integer> counter = new HashMap<String, Integer>();
        for (int i = 0; i < words.length; i++) {
            if (!counter.containsKey(words[i])) {
                counter.put(words[i], 1);
            } else {
                counter.put(words[i], counter.get(words[i]) + 1);
            }
        }
        tests.put(name, counter);
    }

    private int readfolder(String foldername) throws IOException {
        File folder = new File(foldername);
        File[] listOfFile = folder.listFiles();
        for (int j = 0; j < listOfFile.length; j++) {
            // System.out.println(listOfFile[j].getName());
            // read subfolders
            if (listOfFile[j].isDirectory()) {
                readfolder(listOfFile[j].getPath());
            } else if (listOfFile[j].getName().startsWith("INDEX-")) {
                readindexfile(listOfFile[j].getParent() + "/" + listOfFile[j].getName());
            }
        }
        return folder.listFiles().length;
    }

    private double cal_distant(HashMap<String, Integer> text1,
            HashMap<String, Integer> text2) {
        double nboftoken = 0.0;
        double sum = 0.0;

        Set<String> keys1 = text1.keySet();
        Set<String> keys2 = text2.keySet();
        Set<String> allkeys = new HashSet<String>();
        allkeys.addAll(keys1);
        allkeys.addAll(keys2);
        Integer Na = 0, Nb = 0;
        // get the nb of token in each text
        for (String key : allkeys) {
            Integer Fa = 0;
            Integer Fb = 0;
            if (text1.containsKey(key)) {
                Fa = text1.get(key);
            }
            if (text2.containsKey(key)) {
                Fb = text2.get(key);
            }
            Na += Fa;
            Nb += Fb;
        }
        // reduce propotion for text of different lenght
        if (Na <= Nb) {
            for (String key : allkeys) {
                Integer Fa = 0;
                Integer Fb = 0;
                if (text1.containsKey(key)) {
                    Fa = text1.get(key);
                }
                if (text2.containsKey(key)) {
                    Fb = text2.get(key);
                }
                sum += Math.abs(Fa - (double) Fb * (Na / (double) Nb));
            }
            return sum / (2 * Na);
        } else {
            for (String key : allkeys) {
                Integer Fa = 0;
                Integer Fb = 0;
                if (text1.containsKey(key)) {
                    Fa = text1.get(key);
                }
                if (text2.containsKey(key)) {
                    Fb = text2.get(key);
                }
                sum += Math.abs(Fa * (Nb / (double) Na) - (double) Fb);
            }
            return sum / (2 * Nb);
        }
    }

    private void readindexfile(String path) throws IOException {
        File index = new File(path);
        BufferedReader br;
        br = new BufferedReader(new FileReader(index));
        String line;
        HashMap<String, Integer> a = new HashMap<String, Integer>();
        while ((line = br.readLine()) != null) {
            String[] b = line.split(" ");
            a.put(b[0], Integer.parseInt(b[1]));
        }
        br.close();

        samples.put(path, a);

    }

    private String find_NN(HashMap<String, Double> distantto) {
        double minNN = 1.0;
        String NN = "";
        for (String key : distantto.keySet()) {
            if (distantto.get(key) <= minNN) {
                NN = key;
                minNN = distantto.get(key);
            }

        }
        // it returns the path to the NN
        return NN;
    }

    private String classified(HashMap<String, HashMap<String, Double>> distant) {
        String filePath = System.getProperty("java.io.tmpdir") + "/alldistant.xls";
        File distantout = new File(filePath);
        PrintWriter out;

        try {
            out = new PrintWriter(distantout);

            for (String key : distant.keySet()) {
                for (String key2 : distant.get(key).keySet()) {
                    out.println(key + "\t" + key2 + "\t" + distant.get(key).get(key2));
                }
            }
            out.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        String key1 = "";
        String NN = "";
        for (String key : distant.keySet()) {
            // find it nearest neighbourgh
            key1 = key;
            NN = find_NN(distant.get(key));
        }
        return (NN + "\t" + distant.get(key1).get(NN));
    }

    private String convert(String pathpdf) throws FileNotFoundException, IOException {
        File pdf = new File(pathpdf);
        File totxt = new File(pdf.getPath()
                .substring(0, pdf.getPath().lastIndexOf('.')) + ".txt");

        PDFTextStripper stripper = new PDFTextStripper();
        PDDocument pd;
        BufferedWriter wr;

        try {
            pd = PDDocument.load(pdf);

            wr = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(totxt)));
            stripper.writeText(pd, wr);
            if (pd != null) {
                pd.close();
            }
            // I use close() to flush the stream.
            wr.close();
        } catch (Exception e) {

            return "error reading pdf";
        }
        String text = "";
        BufferedReader br;
        br = new BufferedReader(new FileReader(totxt));
        String line;
        while ((line = br.readLine()) != null) {
            text += line;
            text += " ";
        }
        br.close();

        return text;

    }
    private HashMap<String, HashMap<String, Integer>> samples = new HashMap<String, HashMap<String, Integer>>();
    private HashMap<String, HashMap<String, Integer>> tests = new HashMap<String, HashMap<String, Integer>>();

    public String upload(String fileName, byte[] imageBytes) {
        HashMap<String, HashMap<String, Double>> distant = new HashMap<String, HashMap<String, Double>>();
        samples.clear();
        tests.clear();
        String filePath = System.getProperty("java.io.tmpdir") + "/" + fileName;

        try {
            FileOutputStream fos = new FileOutputStream(filePath);
            BufferedOutputStream outputStream = new BufferedOutputStream(fos);
            outputStream.write(imageBytes);
            outputStream.close();

        } catch (IOException ex) {
            System.err.println(ex);
            throw new WebServiceException(ex);
        }
        try {
            String relativeWebPath = "/WEB-INF/data2";
          
ServletContext servletContext =
    (ServletContext) context.getMessageContext().get(MessageContext.SERVLET_CONTEXT);
  String absoluteDiskPath = servletContext.getRealPath(relativeWebPath);
            readfolder(absoluteDiskPath);
            normalize(fileName, convert(filePath));

            for (String key : tests.keySet()) {
                HashMap<String, Double> distantto = new HashMap<String, Double>();
                for (String key2 : samples.keySet()) {
                    double distanttt = cal_distant(tests.get(key),
                            samples.get(key2));

                    distantto.put(key2, distanttt);

                }
                distant.put(key, distantto);
            }
            return fileName + "\t" + classified(distant);

        } catch (IOException ex) {
            Logger.getLogger(Checker.class.getName()).log(Level.SEVERE, null, ex);
        }
        return filePath;
    }

    @WebMethod
    public String downloadresult() {

        String filePath = System.getProperty("java.io.tmpdir") + "/alldistant.xls";
        try {
            File file = new File(filePath);
            FileInputStream fis = new FileInputStream(file);
            BufferedInputStream inputStream = new BufferedInputStream(fis);

            BufferedReader br = new BufferedReader(new FileReader(file));
            String alldistant = new String();
            String line;
            while ((line = br.readLine()) != null) {
                alldistant += line + "\n";

            }

//            byte[] fileBytes = new byte[(int) file.length()];
//            inputStream.read(fileBytes);
//            inputStream.close();
            return alldistant;
        } catch (IOException ex) {
            System.err.println(ex);
            throw new WebServiceException(ex);
        }
    }

    @WebMethod(operationName = "hello")
    public String hello(@WebParam(name = "name") String txt) {
        return "Hello " + txt + " !";
    }
}
