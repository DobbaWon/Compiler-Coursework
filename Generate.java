public class Generate extends AbstractGenerate {

    @Override
    public void reportError(Token token, String explanatoryMessage) throws CompilationException {
        String errorMessage = "Syntax Error on line " + token.lineNumber + ": " + explanatoryMessage;

        System.err.println(errorMessage);

        throw new CompilationException(errorMessage);
    }
}