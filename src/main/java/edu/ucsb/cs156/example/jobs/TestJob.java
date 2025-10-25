package edu.ucsb.cs156.example.jobs;

import edu.ucsb.cs156.example.services.jobs.JobContext;
import edu.ucsb.cs156.example.services.jobs.JobContextConsumer;
import lombok.Builder;

@Builder
public class TestJob implements JobContextConsumer {

  private boolean fail;
  private int sleepMs;

  @Override
  public void accept(JobContext ctx) throws Exception {
    ctx.log("Hello World! from test job!");
    Thread.sleep(sleepMs);
    if (fail) {
      throw new Exception("Fail!");
    }
    ctx.log("Goodbye from test job!");
  }
}
