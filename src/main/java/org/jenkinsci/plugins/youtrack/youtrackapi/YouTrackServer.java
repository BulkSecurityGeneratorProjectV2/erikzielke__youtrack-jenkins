package org.jenkinsci.plugins.youtrack.youtrackapi;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import net.sf.json.util.JSONUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class YouTrackServer {
    private String serverUrl;

    public YouTrackServer(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public List<Project> getProjects(User user) {
        List<Project> projects = new ArrayList<Project>();
        try {
            URL url = new URL(serverUrl + "/rest/project/all");
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();


            for (String cookie : user.getCookies()) {

                urlConnection.setRequestProperty("Cookie", cookie);
            }

            SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
            try {
                SAXParser saxParser = saxParserFactory.newSAXParser();
                MyDefaultHandler dh = new MyDefaultHandler();
                saxParser.parse(urlConnection.getInputStream(), dh);
                return dh.getProjects();
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
            }


        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return projects;

    }

    public void comment(User user, Issue issue, String comment) {
        try {
            URL url = new URL(serverUrl + "/rest/issue/"+issue.getId()+"/execute");
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);

            for (String cookie : user.getCookies()) {
                urlConnection.setRequestProperty("Cookie", cookie);
            }

            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(urlConnection.getOutputStream());
            outputStreamWriter.write("comment=" + comment);
            outputStreamWriter.flush();

            int responseCode = urlConnection.getResponseCode();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void applyCommand(User user, Issue issue, String command, String comment, User runAs) {
        try {
            URL url = new URL(serverUrl + "/rest/issue/"+issue.getId()+"/execute");
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);

            for (String cookie : user.getCookies()) {
                urlConnection.setRequestProperty("Cookie", cookie);
            }

            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(urlConnection.getOutputStream());
            String str = "comment=" + comment + "&command=" + command;
            if(runAs != null) {
                str += "&runAs=" + user.getUsername();
            }
            outputStreamWriter.write(str);
            outputStreamWriter.flush();

            int responseCode = urlConnection.getResponseCode();
            System.out.println(responseCode);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public User login(String username, String password) {
        try {
            URL url = new URL(serverUrl + "/rest/user/login");
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(urlConnection.getOutputStream());
            outputStreamWriter.write("login=" + username + "&password=" + password);
            outputStreamWriter.flush();

            Map<String, List<String>> headerFields = urlConnection.getHeaderFields();
            List<String> strings = headerFields.get("Set-Cookie");

            String headerField = "";
            User user = new User();
            for (String string : strings) {
                user.getCookies().add(string);
            }

            return user;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static class MyDefaultHandler extends DefaultHandler {
        private List<Project> projects;
        private Project current;

        public List<Project> getProjects() {
            return projects;
        }

        @Override
        public void startDocument() throws SAXException {
            super.startDocument();
            this.projects = new ArrayList<Project>();
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            super.startElement(uri, localName, qName, attributes);
            if (qName.equals("project")) {
                Project project = new Project();
                project.setShortName(attributes.getValue("shortName"));
                projects.add(project);
            }
        }
    }
}