package com.perunlabs.mokosh.pipeline;

import static com.perunlabs.mokosh.pipe.BlockingPipe.blockingPipe;
import static com.perunlabs.mokosh.pipe.Buffer.buffer;
import static com.perunlabs.mokosh.pipeline.Pipeline.pipeline;
import static com.perunlabs.mokosh.testing.Testing.collectToList;
import static java.util.Arrays.asList;
import static org.testory.Testory.given;
import static org.testory.Testory.givenTest;
import static org.testory.Testory.thenReturned;
import static org.testory.Testory.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;

import com.perunlabs.mokosh.pipe.Pipe;

public class TestPipeline {
  private Pipe<Object> pipe;
  private ByteArrayOutputStream output;
  private Iterator<Integer> iterator;

  @Before
  public void before() {
    givenTest(this);
    given(pipe = blockingPipe(Integer.MAX_VALUE));
    given(output = new ByteArrayOutputStream());
  }

  @Test
  public void runs_iterator_to_list() {
    when(pipeline(asList(1, 2, 3).iterator())
        .run()
        .await()
        .get());
    thenReturned(asList(1, 2, 3));
  }

  @Test
  public void runs_iterator_to_output() {
    given(pipeline(asList(1, 2, 3).iterator())
        .run(pipe.output())
        .await());
    when(collectToList(pipe.input()));
    thenReturned(asList(1, 2, 3));
  }

  @Test
  public void runs_output_to_list() {
    given(iterator = asList(1, 2, 3).iterator());
    when(pipeline(output -> output.connect(iterator), blockingPipe(1))
        .run()
        .await()
        .get());
    thenReturned(asList(1, 2, 3));
  }

  @Test
  public void runs_input_stream_to_byte_array() {
    when(pipeline(new ByteArrayInputStream(new byte[] { 1, 2, 3 }))
        .run()
        .await()
        .get());
    thenReturned(new byte[] { 1, 2, 3 });
  }

  @Test
  public void runs_output_stream_to_byte_array() {
    when(pipeline(output -> {
      try {
        output.write(new byte[] { 1, 2, 3 });
        output.close();
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }, buffer(1))
        .run()
        .await()
        .get());
    thenReturned(new byte[] { 1, 2, 3 });
  }

  @Test
  public void runs_byte_array_to_output_stream() {
    given(pipeline(new ByteArrayInputStream(new byte[] { 1, 2, 3 }))
        .run(output)
        .await());
    when(output.toByteArray());
    thenReturned(new byte[] { 1, 2, 3 });
  }

  @Test
  public void maps_object_to_object() {
    when(pipeline(asList(1, 2, 3).iterator())
        .map(integer -> Integer.toString(integer), blockingPipe(1))
        .run()
        .await()
        .get());
    thenReturned(asList("1", "2", "3"));
  }

  @Test
  public void maps_object_to_stream() {
    when(pipeline(asList(1, 2, 3).iterator())
        .map((iterator, output) -> {
          try {
            while (iterator.hasNext()) {
              output.write(iterator.next());
            }
            output.close();
          } catch (IOException e) {
            throw new UncheckedIOException(e);
          }
        }, buffer(1))
        .run()
        .await()
        .get());
    thenReturned(new byte[] { 1, 2, 3 });
  }

  @Test
  public void maps_stream_to_object() {
    when(pipeline(new ByteArrayInputStream(new byte[] { 1, 2, 3 }))
        .map((input, output) -> {
          output.connect(new Iterator<Integer>() {
            public boolean hasNext() {
              try {
                return input.available() > 0;
              } catch (IOException e) {
                throw new UncheckedIOException(e);
              }
            }

            public Integer next() {
              try {
                return input.read();
              } catch (IOException e) {
                throw new UncheckedIOException(e);
              }
            }
          });
        }, pipe)
        .run()
        .await()
        .get());
    thenReturned(asList(1, 2, 3));
  }

  @Test
  public void maps_stream_to_stream() {
    when(pipeline(new ByteArrayInputStream(new byte[] { 1, 2, 3 }))
        .map((input, output) -> {
          output.connect(new Iterator<Integer>() {
            public boolean hasNext() {
              try {
                return input.available() > 0;
              } catch (IOException e) {
                throw new UncheckedIOException(e);
              }
            }

            public Integer next() {
              try {
                return input.read();
              } catch (IOException e) {
                throw new UncheckedIOException(e);
              }
            }
          });
        }, pipe)
        .run()
        .await()
        .get());
    thenReturned(asList(1, 2, 3));
  }

  @Test
  public void runs_with_two_mappers() {
    when(pipeline(asList(1, 2, 3).iterator())
        .map(integer -> Integer.toString(integer), blockingPipe(1))
        .map(string -> Integer.parseInt(string), blockingPipe(1))
        .run()
        .await()
        .get());
    thenReturned(asList(1, 2, 3));
  }
}
