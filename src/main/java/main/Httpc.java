package main;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.io.*;
import java.util.Scanner;

@Command(name = "httpc", mixinStandardHelpOptions = false, subcommands = { get.class, help.class, post.class },
        version = "httpc 1.0", description = "HTTPC CLI")

final public class Httpc implements Runnable{

    @Override
    public void run() {}

    public static void main(String[] args) throws IOException {

        String input = "";

        String URL = null, newURL, protocol = "http://", data = " ";
        boolean isInlineData = false, isFileData = false;
        File file;
        Scanner in = new Scanner(System.in);
        input = in.nextLine();
        input = input.trim();
        //GET
        if(input.startsWith("get")){
//            //System.out.println("begins with get!\n");
            if (input.contains(protocol)) {                                             //valid http://
                //Adding protocol command before the URL in the input string to be parsed
                if ((input.contains("-h")) || (input.contains("-v"))) {
                    URL = input.substring(input.lastIndexOf(" ") + 1);              //find location of URL
                    input = input.substring(0, input.lastIndexOf(" "));             //remove URL from String input
                    //reconstruct url
                    newURL = " " + protocol + " " + URL;                                //attach protocol so get subcommand can recognize url
//                    //System.out.println("before concat " + input);
//                    //System.out.println(URL);

                } else {
                    URL = input.substring(input.indexOf(protocol));                     //find location of URL
                    input = input.substring(0, input.indexOf(protocol));                //remove URL from String input
                    //reconstruct url
                    newURL = protocol + " " + URL;                                      //attach protocol so get subcommand can recognize url
//                    //System.out.println(URL + "\n" + input);
                }

                input = input + newURL;                                                 //concatenate the protocol + url to the input string to be parsed
//                //System.out.println("after concat " + input);
            }
            else if (!(input.contains(protocol)))                                       // handles invalid URL (missing http://) - displays help get usage message
            {
                int exitCode = new CommandLine(new Httpc()).execute("help", "get");
                System.exit(exitCode);
            }
            String[] parsedInput = input.split(" ");                              //parse string input to feed to cmd line httpc constructor
            for (String element: parsedInput) {
                System.out.println(element);
            }
            int exitCode = new CommandLine(new Httpc()).execute(parsedInput);
            System.exit(exitCode);
        }
        //POST
        else if(input.startsWith("post")){
            //System.out.println("begins with post!\n");
            if ((!(input.contains(protocol))) || ((input.contains("-d")) && (input.contains("-f"))))            // handles invalid URL (missing http://) and both -d and -f missing- displays help get usage message
            {
                int exitCode = new CommandLine(new Httpc()).execute("help", "post");
                System.exit(exitCode);
            }
            else if (input.contains(protocol)) {                                        //valid http://
                //Remove the URL in the input string to be parsed
                if ((input.contains("-h")) || (input.contains("-v")) ||
                        (input.contains("-d")) || (input.contains("-f"))) {
                    URL = input.substring(input.lastIndexOf(" ") + 1);              //find location of URL
                    input = input.substring(0, input.lastIndexOf(" "));             //remove URL from String input
                    //newURL = " " + protocol + " " + URL;                              //to attach protocol so get subcommand can recognize url
//                    //System.out.println("before concat " + input);
//                    //System.out.println(URL);
                } else {
                    URL = input.substring(input.indexOf(protocol));                     //find location of URL
                    input = input.substring(0, input.indexOf(protocol));                //remove URL from String input
                    //reconstruct url
                    //newURL = protocol + " " + URL;                                     //to attach protocol so get subcommand can recognize url
//                    //System.out.println(URL + "\n" + input);
                }

                //Handling inline-data VS file
                if(input.contains("-d")){
                    isInlineData = true;
                    //read everything in between ' ' into a string to avoid parsing issues
                    data= input.substring(input.indexOf("'")+1, input.lastIndexOf("'"));          //create a new string for the data so it can be passed as a parameter to httpc.execute
                    input = input.substring(0, input.indexOf("-d"));
                    data += "\r\n";
                    //remove -d <args> from input string to be parsed
                    //System.out.println(input);
                    //System.out.println(data);
                }
                if(input.contains("-f")){
                    isFileData = true;                                                                  //keep input string as is
                    String fileName = input.substring(input.indexOf("-f") + 3);
                    input = input.substring(0, input.indexOf("-f"));                                    //remove -f <fileName> from input string to be parsed
                    //System.out.println("-------------" + input);

                    FileReader fr = new FileReader(fileName);
                    BufferedReader br = new BufferedReader(fr);
                    String line = br.readLine();
                    //System.out.println("----" + line);
                    data = "";
                    if (line == null){                                                               //handles empty file
                        data = " ";
                    }
                    while(line != null) {
                        //System.out.println(line);
                        data += line + "\r\n";
                        line = br.readLine();
                    }
                    br.close();
                    //System.out.println(data);
                }
            }

            String[] parsedInput = input.split(" ");                              //parse string input to feed to cmd line httpc constructor
            //System.out.println(parsedInput.length);
            String[] arguments = new String[parsedInput.length + 4];

            for (int i = 0; i < parsedInput.length; i++) {
                arguments[i] = parsedInput[i];
                //System.out.println(arguments[i]);
            }
            if((isInlineData)  || (!isInlineData && !isFileData)){
                arguments[arguments.length - 4] = "-d";                                 //data coming from inline OR no inline or file data specified, added to arguments array to be passed to httpc
                arguments[arguments.length - 3] = data;
            }
            if(isFileData) {                                                      //data coming from file added to arguments array to be passed to httpc
                arguments[arguments.length - 4] = "-f";
                arguments[arguments.length - 3] = data;
            }
            arguments[arguments.length - 2] = protocol;
            arguments[arguments.length - 1] = URL;

            //System.out.println(arguments.length);
            //System.out.println(arguments[arguments.length - 4]);
            //System.out.println(arguments[arguments.length - 3]);
            //System.out.println(arguments[arguments.length - 2]);
            //System.out.println(arguments[arguments.length - 1]);

            int exitCode = new CommandLine(new Httpc()).execute(arguments);
            System.exit(exitCode);
        }
        else{                                                                           //Handles invalid input and displays help message
            if(!(input.startsWith("help")))
                input = "help";
            String[] parsedInput = input.split(" ");
            int exitCode = new CommandLine(new Httpc()).execute(parsedInput);
            System.exit(exitCode);
        }
    }
}
