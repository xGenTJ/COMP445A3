package main;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "help", description = "Help command matching criteria")
final public class help implements Runnable{

    @Option(names = {"get", "GET"}, description = "Display the usage message")
    boolean isHelpGet;

    @Option(names = {"post", "POST"}, description = "Display the usage message")
    boolean isHelpPost;

    @Option(names = {"server"}, description = "Display the usage message")
    boolean isHelpServer;

    @Override
    public void run(){
        System.out.println("httpc help command running...");
        String helpMsg = "httpc is a curl-like application but supports HTTP protocol only.\n" +
                "Usage:\n" +
                "\thttpc command [arguments]\n" +
                "The commands are:\n" +
                "\tget\t\tExecutes a HTTP GET request and prints the response.\n" +
                "\tpost\tExecutes a HTTP POST request and prints the response.\n" +
                "\thelp\tPrints this screen\n\n" +
                "Use \"httpc help [command]\" for more information about a command.\n";
        String helpGETMsg = "httpc help get\n" +
                "Usage: \n" +
                "\thttpc get [-v] [-h key:value] URL\n" +
                "Get executes a HTTP GET request for a given URL.\n" +
                "\t-v\t\t\t\tPrints the details of the response such as protocol, status and headers.\n" +
                "\t-h key:value\tAssociates headers to HTTP request with the format \'key:value\'.\n";
        String helpPOSTMsg = "httpc help post\n" +
                "Usage: \n" +
                "\thttpc post [-v] [-h key:value] [-d inline-data] [-f file] URL\n" +
                "Post executes a HTTP POST request for a given URL.\n" +
                "\t-v\t\t\t\tPrints the details of the response such as protocol, status and headers.\n" +
                "\t-h key:value\tAssociates headers to HTTP request with the format \'key:value\'.\n" +
                "\t-d string\t\tAssociates inline data to the body HTTP POST request.\n" +
                "\t-f file\t\t\tAssociates the content of a file to the body HTTP POST request.\n\n" +
                "Either [-d] or [-f] can be used but not both.\n";
        String helpServer = "httpfs is a simple file server.\n" +
                "Usage:\n" +
                "\thttpfs command [-v] [-p PORT] [-d PATH-TO-DIR]\n" +
                "The commands are:\n" +
                "\t-v\tPrints debugging messages.\n" +
                "\t-p\tSpecifies the port number that the server will listen and serve at.\n" +
                "\t\tDefault is 80.\n" +
                "\t-d\tSpecifies the directory that the server will use to read/write\n" +
                "\t\trequested files. Default is the current directory when launching the\n" +
                "\t\tapplication.\n\n";

        if(!(isHelpGet) && !(isHelpPost) && !(isHelpServer)){ System.out.println(helpMsg); }
        if(isHelpGet) { System.out.print(helpGETMsg); }
        if(isHelpPost) { System.out.print(helpPOSTMsg); }
        if(isHelpServer) { System.out.print(helpServer); }
    }
}
