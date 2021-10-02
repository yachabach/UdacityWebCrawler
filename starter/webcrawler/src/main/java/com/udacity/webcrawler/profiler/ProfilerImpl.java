package com.udacity.webcrawler.profiler;

import javax.inject.Inject;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Objects;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

/**
 * Concrete implementation of the {@link Profiler}.
 *
 */
final class ProfilerImpl implements Profiler {

  private final Clock clock;
  private final ProfilingState state = new ProfilingState();
  private final ZonedDateTime startTime;

  @Inject
  ProfilerImpl(Clock clock) {
    this.clock = Objects.requireNonNull(clock);
    this.startTime = ZonedDateTime.now(clock);
  }

  /**
   *
   * @param klass    the class object representing the interface of the delegate.
   * @param delegate the object that should be profiled.
   * @param <T> generic type of klass object
   * @return the wrapped function to be used
   *
   * <p>This method is called in PageParserFactoryImpl.  It is also called in several
   * testing routines.</p>
   */
  @Override
  public <T> T wrap(Class<T> klass, T delegate) throws IllegalArgumentException {
    Objects.requireNonNull(klass);

    // TODO: Use a dynamic proxy (java.lang.reflect.Proxy) to "wrap" the delegate in a
    //       ProfilingMethodInterceptor and return a dynamic proxy from this method.
    //       See https://docs.oracle.com/javase/10/docs/api/java/lang/reflect/Proxy.html.

    /*
    klass is the blueprint.  delegate is the object (the instantiation of
    klass).  We use klass to tell the proxy how to load the object and
    what interfaces the object will use.  The method interceptor needs
    the delegate.  The newProxyInstance does not.
     */

    if (!isAnyMethodProfiled(klass))
      throw new IllegalArgumentException("No methods annotated with @Profiled");

    T proxy = (T) Proxy.newProxyInstance(klass.getClassLoader(),
              new Class<?>[]{klass},
              new ProfilingMethodInterceptor(clock, delegate, state));

    return proxy;
  }

  private boolean isAnyMethodProfiled(Class<?> klass) {
    Method[] methods = klass.getMethods();
    for (Method m:methods){
      if (m.getAnnotation(Profiled.class) != null) return true;
    }
    return false;
  }

  @Override
  public void writeData(Path path) {
    // TODO: Write the ProfilingState data to the given file path. If a file already exists at that
    //       path, the new data should be appended to the existing file.

    //Redo this
    try {
      Files.write(path, (state.toString() + "\n").getBytes(),
              StandardOpenOption.CREATE,
              StandardOpenOption.WRITE,
              StandardOpenOption.APPEND);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  @Override
  public void writeData(Writer writer) throws IOException {
    writer.write("Run at " + RFC_1123_DATE_TIME.format(startTime));
    writer.write(System.lineSeparator());
    state.write(writer);
    writer.write(System.lineSeparator());
  }
}
