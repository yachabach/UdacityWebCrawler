package com.udacity.webcrawler.profiler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * A method interceptor that checks whether {@link Method}s are annotated with the {@link Profiled}
 * annotation. If they are, the method interceptor records how long the method invocation took.
 * The lessons called this an invocation handler, not a method interceptor.
 */
final class ProfilingMethodInterceptor implements InvocationHandler {

  private final Clock clock;
  private final Object target;
  private final ProfilingState state;

  // TODO: You will need to add more instance fields and constructor arguments to this class.
  ProfilingMethodInterceptor(Clock clock,
                             Object target, //The wrap method called this delegate
                             ProfilingState state) {
    this.clock = Objects.requireNonNull(clock);
    this.target = Objects.requireNonNull(target);
    this.state = Objects.requireNonNull(state);
  }

  /*
  These verbose comments are for me (the student).  I am struggling to fully
  understand proxy objects.

  When a method in a proxy object is called, this is the function that handles
  that method invocation.  The method that was called is passed in 'method' and
  the arguments for that method live in the array called 'args'.
   */
  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    // TODO: This method interceptor should inspect the called method to see if it is a profiled
    //       method. For profiled methods, the interceptor should record the start time, then
    //       invoke the method using the object that is being profiled. Finally, for profiled
    //       methods, the interceptor should record how long the method call took, using the
    //       ProfilingState methods.

    Object result = null;
    //Check that the called method has a profiled decorator
    if (method.isAnnotationPresent(Profiled.class)) {
      //Start the timer
      final Instant startTime = clock.instant();

      //Invoke the method while catching and passing on exceptions
      try {
        result = method.invoke(target, args);
      } catch (InvocationTargetException e) {
        throw e.getTargetException();
      } catch (Exception e) {  // IllegalAccessException e) {
        throw e;  //new RuntimeException(e);
      } finally {
        //Record the execution time
        state.record(target.getClass(), method,
                Duration.between(startTime, clock.instant()));
      }

      //If the method is not annotated, run it and return the result
    } else result = method.invoke(target, args);
    return result;
  }
}
