package com.udacity.webcrawler.profiler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * A method interceptor that checks whether {@link Method}s are annotated with the {@link Profiled}
 * annotation. If they are, the method interceptor records how long the method invocation took.
 * The lessons called this an invocation handler, not a method interceptor.
 */
final class ProfilingMethodInterceptor implements InvocationHandler {

  private final Clock clock;
  private ProfilingState state = new ProfilingState();
  private final ZonedDateTime startTime;

  // TODO: You will need to add more instance fields and constructor arguments to this class.
  ProfilingMethodInterceptor(Clock clock,
                             ProfilingState state,
                             ZonedDateTime startTime) {
    this.clock = Objects.requireNonNull(clock);
    this.state = Objects.requireNonNull(state);
    this.startTime = Objects.requireNonNull(startTime);

  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) {
    // TODO: This method interceptor should inspect the called method to see if it is a profiled
    //       method. For profiled methods, the interceptor should record the start time, then
    //       invoke the method using the object that is being profiled. Finally, for profiled
    //       methods, the interceptor should record how long the method call took, using the
    //       ProfilingState methods.

    /*From Udacisearch example
    // TODO: Fill this method in!

    String methodName = method.getName(); //This will be 'get'-something/

      /*
      This part is very confusing and poorly explained.  Basically, if the method
      throws an exception then this code re-throws the exception.  I don't understand
      how we get here or why.
       */
    /*if (method.getDeclaringClass().equals(Object.class)) {
      try {
        return method.invoke(properties, args);
      } catch (InvocationTargetException e) {
        throw e.getTargetException();
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      }

    }

    //Here we make sure it's a valid property name
    if (methodName.length() <= 3 || !methodName.startsWith("get")) {
      throw new RuntimeException("Method is not a property getter: " + methodName);
    }

    //Filter the 'get' out of the method name to end up with a property name (hopefully)
    String propertyName = methodName.substring(3, 4).toLowerCase() + methodName.substring(4);

    //If the property doesn't exist we throw a runtime error.
    if (!properties.containsKey(propertyName)) {
      throw new RuntimeException("No property named \"" + propertyName + "\" found in map.");
    }

      /*
      Here is where the real adapting happens.  Instead of using the this.propertyName to return
      a state value, we retrieve the value from the properties map using the property name as the
      index.
       */
    //return properties.get(propertyName);    }*/
    return null;
  }
}
