import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.PrintStream;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import org.fusesource.jansi.Ansi.Color;

import claw.rct.network.low.ConsoleManager;

public class LocalConsoleManager implements ConsoleManager {
    
    private final BufferedInputStream inputStream;
    private final PrintStream out;
    
    public LocalConsoleManager () {
        AnsiConsole.systemInstall();
        inputStream = new BufferedInputStream(System.in);
        out = AnsiConsole.out();
    }
    
    @Override
    public String readInputLine () {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        int lastByte = 0;
        
        while (lastByte != (int)'\n') {
            try {
                lastByte = inputStream.read();
                if (lastByte == -1) throw new EOFException();
            } catch (IOException e) {
                return "";
            }
            bytes.write(lastByte);
        }
        
        return bytes.toString();
    }
    
    @Override
    public boolean hasInputReady () {
        try {
            return inputStream.available() > 0;
        } catch (IOException e) {
            return false;
        }
    }
    
    @Override
    public void flush () {
        System.out.flush();
    }
    
    @Override
    public void print (String msg) {
        out.print(msg);
    }
    
    @Override
    public void printErr (String msg) {
        out.print(Ansi.ansi().fgRed().a(msg).fg(Color.WHITE));
    }
    
    @Override
    public void printSys (String msg) {
        out.print(Ansi.ansi().fgYellow().a(msg).fg(Color.WHITE));
    }
    
    @Override
    public void clear () {
        try {
            new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
        } catch (Exception e) {
            println("\n".repeat(20));
        }
    }
    
    @Override
    public void moveUp (int lines) {
        out.print(Ansi.ansi().cursorUpLine(lines));
    }
    
    @Override
    public void clearLine () {
        out.print(Ansi.ansi().eraseLine());
    }
    
    @Override
    public void saveCursorPos () {
        out.print(Ansi.ansi().saveCursorPosition());
    }
    
    @Override
    public void restoreCursorPos () {
        out.print(Ansi.ansi().restoreCursorPosition());
    }
    
}
