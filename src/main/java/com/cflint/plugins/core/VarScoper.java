package com.cflint.plugins.core;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cflint.BugList;
import com.cflint.CF;
import com.cflint.plugins.CFLintScannerAdapter;
import com.cflint.plugins.Context;

import cfml.parsing.cfscript.CFExpression;
import cfml.parsing.cfscript.CFFullVarExpression;
import cfml.parsing.cfscript.CFIdentifier;
import cfml.parsing.cfscript.script.CFPropertyStatement;
import cfml.parsing.cfscript.script.CFScriptStatement;
import net.htmlparser.jericho.Element;

public class VarScoper extends CFLintScannerAdapter {

    public static final String VARIABLE = "variable";
    public static final String RESULT = "result";

    private final Map<String, List<String>> checkElementAttributes = new HashMap<String, List<String>>();
    private final List<String> checkNames = Arrays.asList(CF.CFQUERY, CF.CFSTOREDPROC, CF.CFFEED, CF.CFDIRECTORY,
        CF.CFFORM, CF.CFFTP, CF.CFOBJECT, CF.CFSEARCH, CF.CFPROCRESULT, CF.CFPOP, CF.CFREGISTRY, CF.CFREPORT,
        CF.CFDBINFO, CF.CFDOCUMENT, CF.CFCOLLECTION, CF.CFPDF, CF.CFZIP, CF.CFLDAP);
    private final Collection<String> scopes = Arrays.asList(CF.APPLICATION, CF.CGI, CF.COOKIE, CF.FORM, CF.REQUEST,
        CF.SERVER, CF.SESSION, CF.URL);

    @Override
    public void expression(final CFExpression expression, final Context context, final BugList bugs) {
        if (expression instanceof CFIdentifier) {
            // No issue
            if (expression instanceof CFFullVarExpression
                    && ((CFFullVarExpression) expression).getExpressions().size() > 1) {
                // Visit the first in the expression.
                expression(((CFFullVarExpression) expression).getExpressions().get(0), context, bugs);
                return;
            }
            final String name = ((CFIdentifier) expression).getName();
            if (context.isInFunction() && context.isInAssignmentExpression()
                && !context.getCallStack().checkVariable(name) && !isGlobal(name)) {
                context.addMessage("MISSING_VAR", name);
            } else if (expression instanceof CFFullVarExpression) {
                final CFFullVarExpression fullVarExpr = (CFFullVarExpression) expression;
                expression(fullVarExpr.getExpressions().get(0), context, bugs);
            }
        }
    }

    @Override
    public void expression(final CFScriptStatement expression, final Context context, final BugList bugs) {
        if (expression instanceof CFPropertyStatement) {
            //final CFPropertyStatement propertyStatement = (CFPropertyStatement)expression;
            //TODO - handle properties?
        }
    }

    public VarScoper() {
        // checkElementAttributes.put(CF.CFLOOP, Arrays.asList(CF.index, CF.ITEM));
        checkElementAttributes.put(CF.CFINVOKE, Arrays.asList(CF.RETURNVARIABLE));
        checkElementAttributes.put(CF.CFFILE, Arrays.asList(VARIABLE));
        checkElementAttributes.put(CF.CFSAVECONTENT, Arrays.asList(VARIABLE));
        checkElementAttributes.put(CF.CFHTTP, Arrays.asList(RESULT));
        checkElementAttributes.put(CF.CFQUERY, Arrays.asList(RESULT));
        checkElementAttributes.put(CF.CFMAIL, Arrays.asList(CF.QUERY));
        checkElementAttributes.put(CF.CFFTP, Arrays.asList(RESULT));
        checkElementAttributes.put(CF.CFWDDX, Arrays.asList(CF.OUTPUT));
        checkElementAttributes.put(CF.CFEXECUTE, Arrays.asList(VARIABLE));
        checkElementAttributes.put(CF.CFNTAUTHENTICATE, Arrays.asList(RESULT));
        checkElementAttributes.put(CF.CFXML, Arrays.asList(VARIABLE));

    }

    @Override
    public void element(final Element element, final Context context, final BugList bugs) {
        final String name = element.getName();
        if (name != null && name.trim().length() > 0 && context.isInFunction()) {
            if (checkNames.contains(name.toLowerCase())) {
                assertVariable(element, context, bugs, element.getAttributeValue(CF.NAME));
            }
            if (checkElementAttributes.containsKey(name.toLowerCase())) {
                for (final String attrName : checkElementAttributes.get(name.toLowerCase())) {
                    assertVariable(element, context, bugs, element.getAttributeValue(attrName));
                }
            }
        }
    }

    protected void assertVariable(final Element element, final Context context, final BugList bugs,
                                  final String inameVar) {
        final String nameVar = inameVar == null ? null : inameVar.split("\\.")[0].split("\\[")[0];
        if (nameVar != null && !context.getCallStack().checkVariable(nameVar) && !isGlobal(nameVar)) {
            context.addMessage("MISSING_VAR", inameVar);
        }
    }

    private boolean isGlobal(final String nameVar) {
        return nameVar != null && scopes.contains(nameVar.toLowerCase().trim());
    }

}
