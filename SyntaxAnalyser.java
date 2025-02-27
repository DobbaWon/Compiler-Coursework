import java.io.IOException;

// '_'s are there to impersonate '<' and '>' in the grammar, but because we can't use them in method declarations, we use '_' instead.


public class SyntaxAnalyser extends AbstractSyntaxAnalyser {
    private String filename;

    public SyntaxAnalyser(String filename) throws IOException {
        lex = new LexicalAnalyser(filename);
        this.filename = filename;
    }
    
    public void acceptTerminal(int symbol) throws IOException, CompilationException {
        if (nextToken.symbol == symbol) {
            myGenerate.insertTerminal(nextToken);
            nextToken = lex.getNextToken();
        } else {
            myGenerate.reportError(nextToken, "Expected " + Token.getName(symbol) + " but found '" + nextToken.text + "' FILE: " + filename);
        }
    }

    // Basically parses the whole file, looks for the begin symbol at the start, and the end symbol at the end, as well as EOF.
    @Override
    public void _statementPart_() throws IOException, CompilationException {
        myGenerate.commenceNonterminal("StatementPart");
        acceptTerminal(Token.beginSymbol);

        // Start parsing the file line by line:
        _statementList_();

        acceptTerminal(Token.endSymbol);
        myGenerate.finishNonterminal("StatementPart");
    }

    // Parses line by line in the file.
    private void _statementList_() throws IOException, CompilationException {
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
    }
    
    

    // Parses a line in the file, starting from the first 'word'(?)
    private void _statement_() throws IOException, CompilationException {
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
    }

    // Sets a variable to be a value, also handling string constants in here:
    private void _assignmentStatement_() throws IOException, CompilationException {
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
    }

    // If Statement
    private void _ifStatement_() throws IOException, CompilationException {
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
    }
    
    // While Statement
    private void _whileStatement_() throws IOException, CompilationException {
        myGenerate.commenceNonterminal("WhileStatement");

        // Very similar logic to If Statement:
        acceptTerminal(Token.whileSymbol);

        _condition_();

        acceptTerminal(Token.loopSymbol);

        _statementList_();

        acceptTerminal(Token.endSymbol);
        acceptTerminal(Token.loopSymbol);

        myGenerate.finishNonterminal("WhileStatement");
    }

    // A method, e.g. 'get', 'put'.
    private void _procedureStatement_() throws IOException, CompilationException {
        myGenerate.commenceNonterminal("ProcedureStatement");

        // Call an identifier and check its arguments:
        acceptTerminal(Token.callSymbol);
        acceptTerminal(Token.identifier);

        _argumentList_();

        myGenerate.finishNonterminal("ProcedureStatement");
    }

    // Do Until Statement
    private void _untilStatement_() throws IOException, CompilationException {
        myGenerate.commenceNonterminal("DoUntilStatement");
    
        // Do a list of statements, until a condition:
        acceptTerminal(Token.doSymbol);

        _statementList_();

        acceptTerminal(Token.untilSymbol);

        _condition_();
    
        myGenerate.finishNonterminal("DoUntilStatement");
    }
    
    // For Statement
    private void _forStatement_() throws IOException, CompilationException {
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
    }
    
    // Gets the list of arguments in any procedure call. Seems like there's never more than one, but just in case.
    private void _argumentList_() throws IOException, CompilationException {
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
    
    }

    // The condition that these statements must follow.
    private void _condition_() throws IOException, CompilationException {
        myGenerate.commenceNonterminal("Condition");

        // Should start with an identifier:
        acceptTerminal(Token.identifier);

        // Check for a conditional operator:
        _conditionalOperator();

        // After the operator, we expect a number constant:
        acceptTerminal(Token.numberConstant);

        myGenerate.finishNonterminal("Condition");
    }

    // Operators used for comparison(?).
    private void _conditionalOperator() throws IOException, CompilationException {
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
    }

    // A list of terms, used for variable assignment:
    private void _expression_() throws IOException, CompilationException {
        myGenerate.commenceNonterminal("Expression");

        _term_();
        while (nextToken.symbol == Token.plusSymbol || nextToken.symbol == Token.minusSymbol) {
            acceptTerminal(nextToken.symbol);
            _expression_();
        }

        myGenerate.finishNonterminal("Expression");
    }

    // A smaller part of an expression, handling the multiplicative operations.
    private void _term_() throws IOException, CompilationException {
        myGenerate.commenceNonterminal("Term");

        // Should start with a number, string or variable:
        _factor_();

        // Then, should be a symbol such as listed below:
        while (nextToken.symbol == Token.timesSymbol
            || nextToken.symbol == Token.divideSymbol 
            || nextToken.symbol == Token.modSymbol) {
            acceptTerminal(nextToken.symbol);

            // Then it should be succeeded by another ter,, and loop back round:
            _term_();
        }

        myGenerate.finishNonterminal("Term");
    }

    // Either a raw number, or a variable reference. (or also a bigger expression (this is confusing))
    private void _factor_() throws IOException, CompilationException {
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
    }
}


