import java.io.IOException;

// '_'s are there to impersonate '<' and '>' in the grammar, but because we can't use them in method declarations, we use '_' instead.


public class SyntaxAnalyser extends AbstractSyntaxAnalyser {
    private String filename;

    public SyntaxAnalyser(String filename) throws IOException {
        lex = new LexicalAnalyser(filename);
        this.filename = filename;
    }
    
    public void acceptTerminal(int symbol) throws IOException, CompilationException {
        try{
            if (nextToken.symbol == symbol) {
                myGenerate.insertTerminal(nextToken);
                nextToken = lex.getNextToken();
            } else {
                myGenerate.reportError(nextToken, "Expected " + Token.getName(symbol) + " but found '" + nextToken.text + "' FILE: " + filename);
            }
        } catch (IOException | CompilationException e){
            System.err.println("While compiling AcceptTerminal");
            throw new CompilationException("Caused by error whilst compiling Terminal on line " + nextToken.lineNumber + "\n" + e.getMessage());
        }
    }

    // Basically parses the whole file, looks for the begin symbol at the start, and the end symbol at the end, as well as EOF.
    @Override
    public void _statementPart_() throws IOException, CompilationException {
    try {   
        myGenerate.commenceNonterminal("StatementPart");
        acceptTerminal(Token.beginSymbol);

        // Start parsing the file line by line:
        _statementList_();

        acceptTerminal(Token.endSymbol);
            myGenerate.finishNonterminal("StatementPart");
        } catch (IOException | CompilationException e){
            System.err.println("While compiling StatementPart");
            throw new CompilationException("Caused by error whilst compiling StatementPart on line " + nextToken.lineNumber + "\n" + e.getMessage());
        }
    }

    // Parses line by line in the file.
    private void _statementList_() throws IOException, CompilationException {
        try{
            while (nextToken.symbol != Token.endSymbol && nextToken.symbol != Token.eofSymbol && nextToken.symbol != Token.elseSymbol) {
                myGenerate.commenceNonterminal("StatementList");
                
                _statement_();
        
                if (nextToken.symbol == Token.semicolonSymbol) {
                    acceptTerminal(Token.semicolonSymbol);
                    _statementList_();
                } else if (nextToken.symbol != Token.endSymbol && nextToken.symbol != Token.elseSymbol && nextToken.symbol != Token.semicolonSymbol) {
                    myGenerate.reportError(nextToken, "Expected ';' or 'end'. FILE: " + filename);
                }
        
                myGenerate.finishNonterminal("StatementList");
            }
        } catch (IOException | CompilationException e){
            System.err.println("While compiling StatementList");
            throw new CompilationException("Caused by error whilst compiling StatementList on line " + nextToken.lineNumber + "\n" + e.getMessage());
        }
    }

    // Parses a line in the file, starting from the first 'word'(?)
    private void _statement_() throws IOException, CompilationException {

        try{
            myGenerate.commenceNonterminal("Statement");

            switch (nextToken.symbol) {
                case Token.callSymbol:
                    _procedureStatement_();
                    break;
                case Token.identifier:
                    _assignmentStatement_();
                    break;
                case Token.whileSymbol:
                    _whileStatement_();
                    break;
                case Token.forSymbol:
                    _forStatement_();
                    break;
                case Token.ifSymbol:
                    _ifStatement_();
                    break;
                case Token.doSymbol:
                    _untilStatement_();
                    break;
                case Token.elseSymbol:
                    myGenerate.reportError(nextToken, "'ELSE' found without 'IF' preceeding. FILE: " + filename);
                default:
                    myGenerate.reportError(nextToken, "Unknown statement. FILE: " + filename);
                    break;
            }
            myGenerate.finishNonterminal("Statement");
        } catch (IOException | CompilationException e){
            System.err.println("While compiling Statement");
            throw new CompilationException("Caused by error whilst compiling Statement on line " + nextToken.lineNumber + "\n" + e.getMessage());
        }
    }

    // Sets a variable to be a value, also handling string constants in here:
    private void _assignmentStatement_() throws IOException, CompilationException {
        try{
            myGenerate.commenceNonterminal("AssignmentStatement");

            // Accept the variable name and then the ':=' symbol:
            acceptTerminal(Token.identifier);
            acceptTerminal(Token.becomesSymbol);

            // check for string constant first:
            if (nextToken.symbol == Token.stringConstant){
                acceptTerminal(Token.stringConstant);
            } else{
                _expression_();
            }

            myGenerate.finishNonterminal("AssignmentStatement");
        } catch (IOException | CompilationException e){
            System.err.println("While compiling AssignmentStatement");
            throw new CompilationException("Caused by error whilst compiling AssignmentStatement on line " + nextToken.lineNumber + "\n" + e.getMessage());
        }
    }

    // If Statement
    private void _ifStatement_() throws IOException, CompilationException {
        try{
            myGenerate.commenceNonterminal("IfStatement");
        
            // Should always start with 'if':
            acceptTerminal(Token.ifSymbol);

            _condition_();

            // After condition comes 'then':
            acceptTerminal(Token.thenSymbol);
            
            // All the stuff in that if statement:
            _statementList_();
        
            // Could be an 'else' symbol:
            if (nextToken.symbol == Token.elseSymbol) {
                acceptTerminal(Token.elseSymbol);
                _statementList_();
            }
        
            // Accept end if:
            acceptTerminal(Token.endSymbol);
            acceptTerminal(Token.ifSymbol);
        
            myGenerate.finishNonterminal("IfStatement");
        } catch (IOException | CompilationException e){
            System.err.println("While compiling IfStatement");
            throw new CompilationException("Caused by error whilst compiling IfStatement on line " + nextToken.lineNumber + "\n" + e.getMessage());
        }
    }
    
    // While Statement
    private void _whileStatement_() throws IOException, CompilationException {
        try{
            myGenerate.commenceNonterminal("WhileStatement");

            // Very similar logic to If Statement:
            acceptTerminal(Token.whileSymbol);

            _condition_();

            acceptTerminal(Token.loopSymbol);

            _statementList_();

            acceptTerminal(Token.endSymbol);
            acceptTerminal(Token.loopSymbol);

            myGenerate.finishNonterminal("WhileStatement");

        } catch (IOException | CompilationException e){
            System.err.println("While compiling WhileStatement");
            throw new CompilationException("Caused by error whilst compiling WhileStatement on line " + nextToken.lineNumber + "\n" + e.getMessage());
        }
    }

    // A method, e.g. 'get', 'put'.
    private void _procedureStatement_() throws IOException, CompilationException {
        try{
            myGenerate.commenceNonterminal("ProcedureStatement");

            // Call an identifier and check its arguments:
            acceptTerminal(Token.callSymbol);
            acceptTerminal(Token.identifier);

            _argumentList_();

            myGenerate.finishNonterminal("ProcedureStatement");
        } catch (IOException | CompilationException e){
            System.err.println("While compiling ProcedureStatement");
            throw new CompilationException("Caused by error whilst compiling ProcedureStatement on line " + nextToken.lineNumber + "\n" + e.getMessage());
        }
    }   

    // Do Until Statement
    private void _untilStatement_() throws IOException, CompilationException {
        try{
            myGenerate.commenceNonterminal("DoUntilStatement");
        
            // Do a list of statements, until a condition:
            acceptTerminal(Token.doSymbol);

            _statementList_();

            acceptTerminal(Token.untilSymbol);

            _condition_();
        
            myGenerate.finishNonterminal("DoUntilStatement");

        } catch (IOException | CompilationException e){
            System.err.println("While compiling DoUntilStatement");
            throw new CompilationException("Caused by error whilst compiling DoUntilStatement on line " + nextToken.lineNumber + "\n" + e.getMessage());
        }
    }
    
    // For Statement
    private void _forStatement_() throws IOException, CompilationException {
        try{
            myGenerate.commenceNonterminal("ForStatement");
        
            // Should start with 'for(':
            acceptTerminal(Token.forSymbol);      
            acceptTerminal(Token.leftParenthesis); 
        
            // The incrementing variable assignment part of the for line:
            acceptTerminal(Token.identifier);
            acceptTerminal(Token.becomesSymbol);
            _expression_();
            acceptTerminal(Token.semicolonSymbol);
        
            // The condition in the middle of the for line:
            _condition_();
            acceptTerminal(Token.semicolonSymbol);
        
            // The incrementing part of the for line (i++ (x1 := x1 + 1)):
            acceptTerminal(Token.identifier);
            acceptTerminal(Token.becomesSymbol);
            _expression_();
        
            // The end of the for line:
            acceptTerminal(Token.rightParenthesis);
            acceptTerminal(Token.doSymbol);      
        
            // All of the statements in the loop body:
            _statementList_();                      
        
            // Should be followed by 'end loop':
            acceptTerminal(Token.endSymbol);
            acceptTerminal(Token.loopSymbol);       
        
            myGenerate.finishNonterminal("ForStatement");
        } catch (IOException | CompilationException e){
            System.err.println("While compiling ForStatement");
            throw new CompilationException("Caused by error whilst compiling ForStatement on line " + nextToken.lineNumber + "\n" + e.getMessage());
        }
    }
    
    // Gets the list of arguments in any procedure call. Seems like there's never more than one, but just in case.
    private void _argumentList_() throws IOException, CompilationException {
        try{
            acceptTerminal(Token.leftParenthesis);
            myGenerate.commenceNonterminal("ArgumentList");
        
            // No arguments:
            if (nextToken.symbol == Token.rightParenthesis) {
                acceptTerminal(Token.rightParenthesis);
            } else {
                // Should start with an identifier, e.g. 'x1':
                if (nextToken.symbol == Token.identifier) {
                    acceptTerminal(Token.identifier);
                    
                    // Keep checking for a comma followed by another argument:
                    while (nextToken.symbol == Token.commaSymbol) {
                        acceptTerminal(Token.commaSymbol);
                        
                        // Each comma should be followed by an identifier:
                        if (nextToken.symbol == Token.identifier) {
                            acceptTerminal(Token.identifier);
                        } else {
                            myGenerate.reportError(nextToken, "Expected an identifier after a comma. FILE: " + filename);
                        }
                    }
                } else {
                    myGenerate.reportError(nextToken, "Expected an identifier as an argument. FILE: " + filename);
                }
        
                // Ensure that the next token after the arguments is a closing parenthesis
                myGenerate.finishNonterminal("ArgumentList");
                acceptTerminal(Token.rightParenthesis);
            }
        } catch (IOException | CompilationException e){
            System.err.println("While compiling ArgumentList");
            throw new CompilationException("Caused by error whilst compiling ArgumentList on line " + nextToken.lineNumber + "\n" + e.getMessage());
        }
    }

    // The condition that these statements must follow.
    private void _condition_() throws IOException, CompilationException {
        try{
            myGenerate.commenceNonterminal("Condition");
        
            // Should start with an identifier:
            acceptTerminal(Token.identifier);
        
            // Check for a conditional operator:
            _conditionalOperator();
        
            // After the operator, we expect either a number constant or an identifier:
            if (nextToken.symbol == Token.numberConstant || nextToken.symbol == Token.identifier) {
                acceptTerminal(nextToken.symbol);
            } else {
                myGenerate.reportError(nextToken, "Expected a number constant or identifier after operator. FILE: " + filename);
            }
        
            myGenerate.finishNonterminal("Condition");

        } catch (IOException | CompilationException e){
            System.err.println("While compiling Condition");
            throw new CompilationException("Caused by error whilst compiling Condition on line " + nextToken.lineNumber + "\n" + e.getMessage());
        }
    }
    

    // Operators used for comparison == != > < >= <= 
    private void _conditionalOperator() throws IOException, CompilationException {
        try{
            myGenerate.commenceNonterminal("ConditionalOperator");

            // Check that it is a valid operator:
            switch (nextToken.symbol) {
                case Token.equalSymbol:
                case Token.notEqualSymbol:
                case Token.lessEqualSymbol:
                case Token.lessThanSymbol:
                case Token.greaterEqualSymbol:
                case Token.greaterThanSymbol:
                    acceptTerminal(nextToken.symbol);
                    myGenerate.finishNonterminal("ConditionalOperator");
                    break;
                default:
                    myGenerate.reportError(nextToken, "Expected a comparison operator but found '" + nextToken.text + "' FILE: " + filename);
                    break;
            }

        } catch (IOException | CompilationException e){
            System.err.println("While compiling Conditional Operator");
            throw new CompilationException("Caused by error whilst compiling ConditionalOperator on line " + nextToken.lineNumber + "\n" + e.getMessage());
        }
    }

    // A list of terms, used for variable assignment:
    private void _expression_() throws IOException, CompilationException {
        try{
            myGenerate.commenceNonterminal("Expression");

            _term_();
            while (nextToken.symbol == Token.plusSymbol || nextToken.symbol == Token.minusSymbol) {
                acceptTerminal(nextToken.symbol);
                _expression_();
            }

            myGenerate.finishNonterminal("Expression");

        } catch (IOException | CompilationException e){
            System.err.println("While compiling Expression");
            throw new CompilationException("Caused by error whilst compiling Expression on line " + nextToken.lineNumber + "\n" + e.getMessage());
        }
    }

    // A smaller part of an expression, handling the multiplicative operations.
    private void _term_() throws IOException, CompilationException {
        try{
            myGenerate.commenceNonterminal("Term");

            // Should start with a number, string or variable:
            _factor_();

            // Then, should be a symbol such as listed below:
            while (nextToken.symbol == Token.timesSymbol
                || nextToken.symbol == Token.divideSymbol 
                || nextToken.symbol == Token.modSymbol) {
                acceptTerminal(nextToken.symbol);

                // Then it should be succeeded by another term, and loop back round:
                _term_();
            }

            myGenerate.finishNonterminal("Term");

        } catch (IOException | CompilationException e){
            System.err.println("While compiling Term");
            throw new CompilationException("Caused by error whilst compiling Term on line " + nextToken.lineNumber + "\n" + e.getMessage());
        }
    }

    // Either a raw number, or a variable reference. (or also a bigger expression (this is confusing))
    private void _factor_() throws IOException, CompilationException {
        try{
            myGenerate.commenceNonterminal("Factor");
        
            // Check if it is either a number, or variable reference:
            switch (nextToken.symbol) {
                case Token.identifier:
                case Token.numberConstant:
                    acceptTerminal(nextToken.symbol);
                    break;
                
                // Now, check if it is a '(':
                case Token.leftParenthesis:
                    acceptTerminal(Token.leftParenthesis);

                    // Parse the expression inside the parentheses:
                    _expression_();
        
                    // Expect a right parenthesis:
                    acceptTerminal(Token.rightParenthesis);
                    break;
        
                default:
                    myGenerate.reportError(nextToken, "Expected an identifier, number, string, or '(' for a parenthesized expression. FILE: " + filename);
                    break;
            }
        
            myGenerate.finishNonterminal("Factor");

        } catch (IOException | CompilationException e){
            System.err.println("While compiling Factor");
            throw new CompilationException("Caused by error whilst compiling Factor on line " + nextToken.lineNumber + "\n" + e.getMessage());
        }
    }
}


