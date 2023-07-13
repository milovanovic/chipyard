package chipyard

import freechips.rocketchip.config.{Config}

class FixedPointQueueConfig extends Config(
  new chipyard.queue.WithFixedPointQueueBlock() ++
  new TinyRocketConfig
)

class UIntQueueConfig extends Config(
  new chipyard.queue.WithUIntQueueBlock() ++
  new TinyRocketConfig
)
class SIntQueueConfig extends Config(
  new chipyard.queue.WithSIntQueueBlock() ++
  new TinyRocketConfig
)