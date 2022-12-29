package rct.network.messages.commands;

import java.io.Serializable;

import rct.network.low.ConsoleManager;
import rct.network.low.ResponseMessage;

/**
 * A {@link Message} object which describes output from the remote command interpreter that controls the local
 * {@link ConsoleManager}.
 */
public class CommandOutputMessage extends ResponseMessage {
    
    public static final long serialVersionUID = 2L;
    
    /**
     * The process ID assigned to this command through the {@link StartCommandMessage}.
     */
    public final int commandProcessId;
    
    /**
     * Whether or not to signal to the driverstation that the command is finished executing.
     */
    public final boolean terminateCommand;
    
    /**
     * Whether or not to request an input line from the driverstation {@link ConsoleManager} after
     * all the {@link ConsoleManagerOperation}s are performed.
     */
    public final boolean requestInputLine;
    
    /**
     * The list of {@link ConsoleManagerOperation}s to perform on the driverstation {@link ConsoleManager}.
     */
    public final ConsoleManagerOperation[] operations;
    
    /**
     * Constructs a new {@link CommandOutputMessage}.
     * @param commandProcessId  See {@link CommandOutputMessage#commandProcessId}.
     * @param terminateCommand  See {@link CommandOutputMessage#terminateCommand}.
     * @param requestInputLine  See {@link CommandOutputMessage#requestInputLine}.
     * @param operations        See {@link CommandOutputMessage#operations}.
     */
    public CommandOutputMessage (int commandProcessId, boolean terminateCommand, boolean requestInputLine, ConsoleManagerOperation[] operations) {
        this.commandProcessId = commandProcessId;
        this.terminateCommand = terminateCommand;
        this.requestInputLine = requestInputLine;
        this.operations = operations;
    }
    
    /**
     * Represents a single operation to perform on the driverstation {@link ConsoleManager},
     * encoded within a {@link CommandOutputMessage} so that the roboRIO can control the local
     * console manager.
     */
    public static class ConsoleManagerOperation implements Serializable {
        
        /**
         * The {@link ConsoleManagerOperationType} type of this operation.
         */
        public final ConsoleManagerOperationType operationType;
        
        /**
         * Only required for the {@link ConsoleManagerOperationType#MOVE_UP}.
         */
        public final int moveUp_lines;
        
        /**
         * Required for the {@link ConsoleManagerOperationType#PRINT}, and the similar
         * {@code PRINT_ERR} and {@code PRINT_SYS} operations.
         */
        public final String print_message;
        
        /**
         * Constructs a new {@link ConsoleManagerOperation}. If arguments are not required
         * for a particular operation, they can be set to any value, but are preferably {@code null}.
         * @param operationType See {@link ConsoleManagerOperation#operationType}.
         * @param moveUp_lines  See {@link ConsoleManagerOperation#moveUp_lines}.
         * @param print_message See {@link ConsoleManagerOperation#print_message}.
         * 
         * @see {@link ConsoleManager}
         */
        public ConsoleManagerOperation (ConsoleManagerOperationType operationType, int moveUp_lines, String print_message) {
            this.operationType = operationType;
            this.moveUp_lines = moveUp_lines;
            this.print_message = print_message;
        }
        
    }
    
    /**
     * Operations which can be performed by the local console manager should appear here.
     */
    public enum ConsoleManagerOperationType {
        
        /**
         * Requires no arguments.
         * @see {@link ConsoleManager#clearWaitingInputLines()}.
         */
        CLEAR_WAITING_INPUT_LINES,
        
        /**
         * Requires the {@code int moveUp_lines} argument.
         * @see {@link ConsoleManager#moveUp(int)}.
         */
        MOVE_UP,
        
        /**
         * Requires no arguments.
         * @see {@link ConsoleManager#clearLine()}.
         */
        CLEAR_LINE,
        
        /**
         * Requires the {@code String print_message} argument.
         * @see {@link ConsoleManager#print(String)}.
         */
        PRINT,
        
        /**
         * Requires the {@code String print_message} argument.
         * @see {@link ConsoleManager#printErr(String)}.
         */
        PRINT_ERR,
        
        /**
         * Requires the {@code String print_message} argument.
         * @see {@link ConsoleManager#printSys(String)}.
         */
        PRINT_SYS,
        
        /**
         * Requires no arguments.
         * @see {@link ConsoleManager#clear()}.
         */
        CLEAR,
    }
    
}
