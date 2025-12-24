package nl.ulso.jxa;

import dagger.Binds;
import dagger.Module;

/// Provides utilities for running JavaScript for Automation (JXA) scripts on macOS.
///
/// JavaScript for Automation arrived in macOS with the release of Yosemite in 2014. It offered
/// JavaScript as an alternative to AppleScript, which felt like a blessing to me. Despite several
/// serious attempts, I never got the hang of AppleScript. For a software engineer like me,
/// AppleScript is the worst. JavaScript made scripting on macOS livable.
///
/// By now, late 2025, the future of JXA, and AppleScript for that matter, is gloomy. "Shortcuts"
/// seem to be the way forward, eventually replacing the older automation system completely. And it
/// makes sense, as Shortcuts runs on multiple platforms and is more secure in many ways. But, I
/// still have so many scripts lying around. And they still work. So, I'll cross that bridge when I
/// get there.
@Module
public abstract class JavaScriptForAutomationModule
{
    @Binds
    abstract JavaScriptForAutomation bindJavaScriptForAutomation(
        JavaScriptForAutomationFromClasspath jxa);
}
