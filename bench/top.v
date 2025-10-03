`timescale 1ns/1ps

module top_tb;

		/*iverilog */
		initial
		begin            
			$dumpfile("top_tb.vcd");
			$dumpvars(0, top_tb);
		end

		reg clk, reset_n;
		wire _clk;
		
		reg [31:0] fmc_data;
		reg [25:0] fmc_addr;
    reg fmc_ne;
    reg fmc_noe;
    reg fmc_nwe;

		wire [31:0] _fmc_data;
		wire [25:0] _fmc_addr;
    wire _fmc_ne;
    wire _fmc_noe;
    wire _fmc_nwe;
		assign _fmc_data = fmc_data;
		assign _fmc_addr = fmc_addr;
		assign _fmc_ne = fmc_ne;
		assign _fmc_noe = fmc_noe;
		assign _fmc_nwe = fmc_nwe;

    FmcApb4Top FmcApb4Top0 (
        .clk(_clk),
        .resetn(reset_n),
        .io_fmc_slave_D(_fmc_data),
        .io_fmc_slave_A(_fmc_addr),
        .io_fmc_slave_NE(_fmc_ne),
        .io_fmc_slave_NOE(_fmc_noe),
        .io_fmc_slave_NWE(_fmc_nwe)
    );

	initial
		begin
			#0.0
				clk = 1;
				reset_n = 0;
				fmc_ne = 1;
				fmc_nwe = 1;
				fmc_noe = 1;
				fmc_addr = 26'h0000;
				//fmc_data = 32'h0000;
			
			#16.0
				reset_n = 1;
			#20.0
				fmc_nwe = 1;
			#20.2
				fmc_ne = 0;
				fmc_addr = 26'h4000;
				fmc_noe = 0;
			#60.0
				fmc_noe = 1;
			

			#15_000 $stop;
		end

	always #2 clk = ~clk;
	assign _clk = clk;

endmodule

