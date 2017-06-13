package com.perunlabs.mokosh.context;

import static com.perunlabs.mokosh.context.InheritableContext.inheritable;
import static com.perunlabs.mokosh.running.Supplying.supplying;
import static org.testory.Testory.given;
import static org.testory.Testory.givenTest;
import static org.testory.Testory.then;
import static org.testory.Testory.thenReturned;
import static org.testory.Testory.when;

import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Test;

import com.perunlabs.mokosh.running.Running;

public class TestInheritable {
  private Context context;
  private Supplier<Foo> supplier, contextSupplier;
  private Foo foo, contextFoo, unknownFoo;

  @Before
  public void before() {
    givenTest(this);
  }

  @Test
  public void context_is_used_by_current_thread() {
    given(context = new Context() {
      public <T> Supplier<T> wrap(Supplier<T> supplier) {
        return () -> supplier.get() == foo
            ? (T) contextFoo
            : (T) unknownFoo;
      }
    });
    given(supplier = () -> foo);
    when(inheritable(context).wrap(supplier).get());
    thenReturned(contextFoo);
  }

  @Test
  public void context_is_used_by_child_thread() {
    given(context = new Context() {
      public <T> Supplier<T> wrap(Supplier<T> supplier) {
        return () -> {
          T org = supplier.get();
          return org == foo
              ? (T) contextFoo
              : (T) org;
        };
      }
    });
    given(supplier = () -> foo);

    Context iContext = inheritable(context);
    Supplier<Boolean> x = iContext.wrap(() -> {
      Running<Foo> dd = supplying(supplier::get);
      Foo sd = dd.await().get();
      boolean rt = sd.equals(contextFoo);
      return rt;
    });

    then(x.get());
  }

  private static class Foo {}
}
