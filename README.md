# HSEA课程作业二: 用RHEA算法玩游戏
## 文件概述
* 采用[GVG-AI](http://www.gvgai.net/)游戏框架
* `src.tracks.singlePlayer.advanced`中有框架提供的RHEA实现`sampleRHEA`
* 自行实现的RHEA在`src.tracks.singlePlayer.advanced.myRHEA`中
* 自行实现的Self-Adaptive RHEA在`src.tracks.singlePlayer.advanced.myAdaptiveRHEA`中
* 测试文件为`src.tracks.singlePlayer.Test.java`

## 游戏选取
|名称|索引|
|---|---|
aliens|0
bomberman|9
jaws|56
roadfighter|80
superman|89

## 实验要求
* 阅读论文, 理解RHEA和Self-Adaptive RHEA的基本流程
* 阅读框架提供的`sampleRHEA`
* 在阅读论文的基础上实现自己的RHEA和Self-Adaptive RHEA, 选择5个游戏在上面进行测试
## 实现方法
### 参考论文
* [Rolling Horizon Evolution versus Tree Search for Navigation in Single-Player Real-Time Games](http://diego-perez.net/papers/GECCO_RollingHorizonEvolution.pdf)
* [Self-Adaptive Rolling Horizon Evolutionary Algorithms for General Video Game Playing](https://rdgain.github.io/assets/pdf/papers/gaina2020onlinerhea.pdf)
  


### `myRHEA`
采用了与sampleRHEA相同的整体框架. 主要的不同之处在以下几个地方.
1.	`TOURNAMENT_SIZE` 改为了3, 这也与论文一中的设计一致.
2.	增加了shift buffer机制. 只在第一次初始化population, 之后沿用上一次的population. 但是首先移除每个individual的第一个动作并向末尾添加一个随机动作. 然后重新评估并排序. 
3.	在`crossover()` 方法中, 对parent selection和crossover的选择进行了改进. 
在`sampleRHEA`中parent selection是通过随机生成`TOURNAMENT_SIZE`个索引, 然后选择索引对应的个体进入tournament进行排序, 取前两个得到的. 而在myRHEA中, 增加了变量`BETTER_PARENT_RANK`, 认为前`BETTER_PARENT_RANK`个个体为”更优个体”; 
4.	并且增加了变量`BETTER_PARENT_PROB`, 在选择个体进入`tournament`时, 以`BETTER_PARENT_PROB`的概率从前`BETTER_PARENT_RANK`个个体中选择. 选择好`TOURNAMENT_SIZE`个个体并排序后, 取前两个进行crossover. 
5.	在`sampRHEA`中, crossover的类型在初始化对象时就被确定, 而在`myRHEA`中, 是随机产生一个crossover的type ID, 然后进行对应的crossover. 
6.	在`Individual.java`中, 增加了2-point crossover, 前m1个和后(actions.length - m2)个action来自parent1, 中间的action来自parent2. 
7.	在`mutate()` 方法中, 突变不再是每次必然发生的, 在`Agent.java`中定义了一个常量MUT_PROB控制突变发生的概率. 

### 添加启发式函数改进`myRHEA`
* 第一个游戏aliens在通用的启发式函数下已经有很好的表现了, 所以在实验中只对后4个游戏设计启发式函数 (在`MyHeuristic.java`文件中). 在`Agent`中通过游戏画面的尺寸  (`getWorldDimension()`) 以及格子尺寸 (`getBlockSize()`) 来确定所玩的游戏是哪一个, 然后相应地初始化MyHeuristic. 调用MyHeuristic中的`evaluate()`方法时, 会根据所玩的游戏调用对应的启发式函数. 为每个游戏添加的启发式函数如下
  * bomberman -- `evalBM()`
  * superman – `evalSM()`
  * jaws – `evalJAWS()`
  * roadfighter – `evalRF()`
* 其中superman在添加了启发式函数后性能反而下降了一些, 其他几个游戏性能上都获得了不同程度的上升. 

### `myAdaptiveRHEA`
* `Tuner`类用于控制参数组合. 该tuner控制的算子为交叉算子, 变异算子, 交叉类型算子和变异位数算子. 其中交叉类型算子下面有uniform crossover, 1-bit crossover和2-bit crossover三种; 变异位数算子可以从0到individual.length-1. 		
* 用`doCrossover` (`boolean`类型) 来决定在`Agent`的`act`方法中是否进行crossover. `doCrossProb` (`double`类型) 确定给`doCrossover`赋值时设为`true`或`false`的概率, 用随机数生成器生成一个随机数, 如果小于`doCrossProb`, 则将`doCrossover`设为`true`, 反之设为`false`. 以下几组变量控制参数的方式亦如此, 分别是: 控制变异算子`doMutation`和`doMutProb`; 控制交叉算子的`crossoverType`和`remainCrossTypeProb`; 控制变异位数的`mutationNum`和`remainMutNumPro`. 
* 是否按照概率改变参数由 `changeParams` (`boolean`类型) 控制. 
* `Tuner`类中的`assignParams`方法按照以上的描述来指派参数. `updateProb`方法用于更新控制参数变量的概率. 如果`nextPop`和`population`的第一个个体的fitness之差`fitnessDiff`大于0, 说明在朝一个好的方向演化, 那么将`changeParams`置为`false`, 下一次指派参数时按照保持当前的参数组合; 如果`fitnessDiff`等于0, 那么将`changeParams`置为`true`, 但是不改变控制参数变量的概率, 在下一次指派参数时会按照当前的概率对参数进行选择; 如果`fitnessDiff`小于0, 说明新一代的质量下降, 那么将`changeParams`置为`true`, 并且更新每一个概率, 为每个概率加或减去一个不超过0.1的数值, 并且将这些概率限制在[0.3, 0.99]的范围内.



 