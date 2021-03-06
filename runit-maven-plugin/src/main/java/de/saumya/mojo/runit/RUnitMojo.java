package de.saumya.mojo.runit;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import de.saumya.mojo.ruby.script.Script;
import de.saumya.mojo.ruby.script.ScriptException;
import de.saumya.mojo.ruby.script.ScriptFactory;
import de.saumya.mojo.runit.JRubyRun.Mode;
import de.saumya.mojo.runit.JRubyRun.Result;

/**
 * maven wrapper around the runit/testcase command.
 *
 * @goal test
 * @phase test
 */
public class RUnitMojo extends AbstractTestMojo {

    enum ResultEnum {
        TESTS, ASSERTIONS, FAILURES, ERRORS, SKIPS
    }

    /**
     * runit directory with glob to be used for the ruby unit command. <br/>
     * Command line -Drunit.dir=...
     *
     * @parameter expression="${runit.dir}" default-value="test/**\/*_test.rb"
     */
    private final String runitDirectory = null;

    /**
     * arguments for the runit command. <br/>
     * Command line -Drunit.args=...
     *
     * @parameter expression="${runit.args}"
     */
    private final String runitArgs = null;

    /**
     * skip the ruby unit tests <br/>
     * Command line -DskipRunit=...
     *
     * @parameter expression="${skipRunit}" default-value="false"
     */
    protected boolean skipRunit;

    private TestResultManager resultManager;
    private File outputfile;
    
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (this.skipTests || this.skipRunit) {
            getLog().info("Skipping RUnit tests");
            return;
        } else {
            outputfile = new File(this.project.getBuild().getDirectory()
                    .replace("${project.basedir}/", ""), "runit.txt");
            resultManager = new TestResultManager(project.getName(), "runit", testReportDirectory, summaryReport);
            super.execute();
        }
    }

    protected Result runIt(ScriptFactory factory, Mode mode, String version, TestScriptFactory scriptFactory)
            throws IOException, ScriptException, MojoExecutionException {

        scriptFactory.setOutputDir(outputfile.getParentFile());
        scriptFactory.setReportPath(outputfile);
        if(runitDirectory.startsWith(launchDirectory().getAbsolutePath())){
            scriptFactory.setSourceDir(new File(runitDirectory));
        }
        else{
            scriptFactory.setSourceDir(new File(launchDirectory(), runitDirectory));
        }

        final Script script = factory.newScript(scriptFactory.getCoreScript());
        if (this.runitArgs != null) {
            script.addArgs(this.runitArgs);
        }
        if (this.args != null) {
            script.addArgs(this.args);
        }

        try {
            script.executeIn(launchDirectory());
        } catch (Exception e) {
            getLog().debug("exception in running tests", e);
        }

        return resultManager.generateReports(mode, version, outputfile);
    }

    @Override
    protected TestScriptFactory newTestScriptFactory(Mode mode) {
        final TestScriptFactory scriptFactory;
        if (mode == Mode._18
                || (mode == Mode.DEFAULT && (jrubySwitches == null || !jrubySwitches
                        .contains("--1.9")))) {
            scriptFactory = new Runit18MavenTestScriptFactory();
        } else {
            scriptFactory = new Runit19MavenTestScriptFactory();
        }
        return scriptFactory;
    }

}
