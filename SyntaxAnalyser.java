import java.io.IOException;
import java.io.PrintStream;

public class SyntaxAnalyser extends AbstractSyntaxAnalyser{
    public SyntaxAnalyser(String text){

    }
    
    @Override
    public void _statementPart_() throws IOException, CompilationException {
        // Start of the program
        myGenerate.commenceNonterminal("StatementPart");
        acceptTerminal(Token.beginSymbol);
        

    }
    
    public void acceptTerminal(int symbol) throws IOException, CompilationException {
        // symbol is the expected symbol for the current state of the program.
        if (nextToken.symbol == symbol) {
            nextToken = lex.getNextToken(); // All is good, move on to the next
        } else {
            myGenerate.reportError(nextToken, "Expected " + Token.getName(symbol) + " but found '" + nextToken.text + "'");
        }
    }
    
}
