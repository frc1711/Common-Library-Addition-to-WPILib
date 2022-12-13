package rct.local;

import java.io.IOException;

import rct.commands.Command;
import rct.network.low.DriverStationSocketHandler;
import rct.network.low.ResponseMessage;
import rct.network.messages.CommandInputMessage;
import rct.network.messages.CommandOutputMessage;
import rct.network.messages.StreamDataMessage;

public class LocalSystem {
    
    // Console manager
    private final ConsoleManager console;
    
    // Command interpreter
    private final LocalCommandInterpreter interpreter;
    
    private CommandInputMessage msgAwaitingResponse = null;
    private int currentRemoteCommandId = 1;
    
    private final Object responseWaiter = new Object();
    private boolean responseReceived;
    private CommandOutputMessage msgReceived;
    private final double responseTimeout;
    
    // Stream data
    private final StreamDataStorage streamDataStorage;
    
    // Socket handling
    private final DriverStationSocketHandler socket;
    
    public LocalSystem (
            int teamNum,
            int remotePort,
            double responseTimeout,
            StreamDataStorage streamDataStorage,
            ConsoleManager console)
            throws IOException {
        
        // Establishing socket connection
        this.console = console;
        console.printlnSys("Connecting to "+DriverStationSocketHandler.getRoborioHost(teamNum)+":"+remotePort+"...");
        try {
            socket = new DriverStationSocketHandler(teamNum, remotePort, this::receiveMessage, this::handleSocketException);
        } catch (IOException e) {
            console.printlnErr("Failed to connect to roboRIO.\n\n");
            throw e;
        }
        console.printlnSys("Successfully connected to roboRIO.");
        
        // Stream data storage
        this.streamDataStorage = streamDataStorage;
        
        // Command interpreter
        interpreter = new LocalCommandInterpreter(console, streamDataStorage);
        this.responseTimeout = responseTimeout;
    }
    
    // SOCKET
    
    private void receiveMessage (ResponseMessage msg) {
        Class<?> msgClass = msg.getClass();
        
        if (msgClass == CommandOutputMessage.class)
            receiveCommandOutputMessage((CommandOutputMessage)msg);
        
        else if (msgClass == StreamDataMessage.class)
            receiveStreamDataMessage((StreamDataMessage)msg);
    }
    
    private void receiveStreamDataMessage (StreamDataMessage msg) {
        streamDataStorage.acceptDataMessage(msg);
    }
    
    private void handleSocketException (IOException e) {
        console.println(e.toString());
    }
    
    /**
     * Closes the socket.
     * @throws IOException See {@link java.net.Socket#close()}
     */
    public void close () throws IOException {
        socket.close();
    }
    
    // COMMAND INTERPRETATION
    
    private void receiveCommandOutputMessage (CommandOutputMessage msg) {
        // Do nothing if there is no message awaiting a response or if the output is
        // for the wrong message
        if (!awaitingRemoteCommandProcess() || msgAwaitingResponse.id != msg.inputCmdMsgId)
            return;
        
        // Wake up the thread waiting for the response
        synchronized (responseWaiter) {
            msgReceived = msg;
            responseReceived = true;
            responseWaiter.notifyAll();
        }
    }
    
    /**
     * Sends a command line to remote to be processed and sets the {@code msgAwaitingResponse} to
     * indicate that we are now awaiting a remote command process. Blocks until the response is
     * received or until a timeout.
     * @param command The command string to send to remote
     * @throws IOException If the socket threw an exception while attempting to send the command or if
     * a response was not received within the timeout period
     */
    private void sendCommandToRemote (String command) throws IOException {
        // Sending the message
        msgAwaitingResponse = new CommandInputMessage(currentRemoteCommandId, command);
        
        socket.sendInstructionMessage(msgAwaitingResponse);
        currentRemoteCommandId ++;
        
        // Wait until the timout or until this thread is interrupted, meaning a response has been received
        responseReceived = false;
        synchronized (responseWaiter) {
            try {
                responseWaiter.wait((long)(responseTimeout * 1000));
            } catch (InterruptedException e) { }
        }
        
        // Throw an exception if the timeout was reached without a message received
        msgAwaitingResponse = null;
        if (!responseReceived) throw new NoResponseException();
    }
    
    /**
     * Checks whether or not the local system is waiting for a remote command process
     * to finish and send back data.
     * @return {@code true} if the local system is awaiting a response, {@code false} otherwise
     */
    public boolean awaitingRemoteCommandProcess () {
        return msgAwaitingResponse != null;
    }
    
    /**
     * Executes a command provided by an input string.
     * <ol>
     * <li>A local command interpreter will attempt to process the command. If it succeeds, then
     * the command has been successfully processed and the method returns here.</li>
     * <li>If the command is not recognized by the local interpreter, it is sent to remote
     * to be processed. A flag is set to indicate that the local system is awaiting a
     * remote command process. See {@link #awaitingRemoteCommandProcess()}. This waiting for
     * a response is blocking.</li>
     * <li>The local system will be expecting a command output from remote.
     * When this happens, {@code awaitingRemoteCommandProcess} will be reset and a
     * new command can be inputted into the local system. The output from the command
     * will also be sent through this local system's {@link ConsoleManager}.</li>
     * </ol>
     * @param line The input string to be processed into a command.
     * @return {@code false} if the command could not be processed because the local system
     * is already waiting on a remote command process, {@code true} otherwise.
     * @throws Command.ParseException If the provided command string if malformed
     * @throws IOException If the command failed to send to remote or no response was received
     */
    public boolean processCommand (String line) throws Command.ParseException, IOException {
        if (awaitingRemoteCommandProcess()) return false;
        
        // Attempt to process the command locally
        if (interpreter.processLine(line)) return true;
        
        // If the command was not successfully processed locally, send it
        // to remote to attempt to process it
        sendCommandToRemote(line);
        
        // Process received message
        if (!msgReceived.isError) {
            console.println(msgReceived.commandOutput);
        } else {
            console.printlnErr(msgReceived.commandOutput);
        }
        
        return true;
    }
    
    public static class NoResponseException extends IOException {
        public NoResponseException () {
            super("No response from the roboRIO was received for the last executed command.");
        }
    }
    
}
